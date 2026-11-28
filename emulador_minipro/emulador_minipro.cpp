#include <iostream>
#include <vector>
#include <string>
#include <cstring>
#include <algorithm>
#include <thread>
#include <mutex>
#include <atomic>
#include <unistd.h>
#include <fcntl.h>
#include <sys/select.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/wait.h>
#include <cstdint>
#include <cstdlib>
#include <cctype>

// Memoria virtual del programador (1MB para simular SPI y EEPROM)
std::vector<uint8_t> virtual_memory(1048576, 0xFF);
std::string flash_file_path = "";
std::string save_file_path = "";

// Modelo de programador emulado. TL866II+, T48 y T56 comparten VID:PID
// a466:0a53 y minipro los distingue por el byte 6 del paquete de estado.
enum ProgrammerModel {
    MODEL_TL866IIPLUS = 5,
    MODEL_T56 = 6,
    MODEL_T48 = 7,
    MODEL_T76 = 8
};

static uint8_t programmer_model = MODEL_TL866IIPLUS;
static uint16_t programmer_vid = 0xa466;
static uint16_t programmer_pid = 0x0a53;
static uint8_t last_control_command = 0x00;

static const char* programmer_name() {
    switch (programmer_model) {
        case MODEL_T48: return "T48";
        case MODEL_T56: return "T56";
        case MODEL_T76: return "T76";
        default: return "TL866II+";
    }
}

static uint8_t programmer_firmware_minor() {
    switch (programmer_model) {
        case MODEL_T48: return 0x27;  // 01.1.39
        case MODEL_T56: return 0x49;  // 01.1.73
        case MODEL_T76: return 0x11;  // 00.1.17
        default: return 143;          // 04.2.143
    }
}

static uint8_t programmer_firmware_major() {
    return programmer_model == MODEL_TL866IIPLUS ? 2 : 1;
}

// Índices de bloque para lectura/escritura entrelazada
uint32_t ep2_read_block = 0;
uint32_t ep3_read_block = 0;
uint32_t ep2_write_block = 0;
uint32_t ep3_write_block = 0;

void load_memory(const std::string& path) {
    FILE* f = fopen(path.c_str(), "rb");
    if (f) {
        size_t r = fread(virtual_memory.data(), 1, virtual_memory.size(), f);
        std::cout << "[MEMORIA] Cargados " << r << " bytes desde " << path << std::endl;
        fclose(f);
    } else {
        std::cout << "[WARN] No se pudo abrir " << path << " para lectura de memoria." << std::endl;
    }
}

void save_memory(const std::string& path) {
    FILE* f = fopen(path.c_str(), "wb");
    if (f) {
        size_t w = fwrite(virtual_memory.data(), 1, virtual_memory.size(), f);
        std::cout << "[MEMORIA] Guardados " << w << " bytes en " << path << std::endl;
        fclose(f);
    } else {
        std::cout << "[WARN] No se pudo abrir " << path << " para escritura de memoria." << std::endl;
    }
}

void fill_memory(const std::string& pattern) {
    if (pattern == "empty" || pattern == "0xff") {
        std::fill(virtual_memory.begin(), virtual_memory.end(), 0xFF);
    } else if (pattern == "count") {
        for (size_t i = 0; i < virtual_memory.size(); i++) {
            virtual_memory[i] = (uint8_t)(i & 0xFF);
        }
    } else if (pattern == "random") {
        srand(time(NULL));
        for (size_t i = 0; i < virtual_memory.size(); i++) {
            virtual_memory[i] = (uint8_t)(rand() & 0xFF);
        }
    }
    std::cout << "[MEMORIA] Rellenada con patrón: " << pattern << std::endl;
}

// Función auxiliar para leer exactamente N bytes
bool read_exactly(int fd, uint8_t* buf, size_t n) {
    size_t total = 0;
    while (total < n) {
        ssize_t r = read(fd, buf + total, n - total);
        if (r <= 0) return false;
        total += r;
    }
    return true;
}

// Lógica de emulación TL866II+ para un socket determinado
void handle_minipro_client(int fd) {
    std::cout << "[MINIPRO-EMU] Cliente conectado al descriptor " << fd << std::endl;

    // Enviar descriptor USB de 18 bytes al inicio para que libusb lo lea
    uint8_t descriptor[18] = {
        18, 1, 0x00, 0x02, 0, 0, 0, 64,
        (uint8_t)(programmer_vid & 0xFF), (uint8_t)(programmer_vid >> 8),
        (uint8_t)(programmer_pid & 0xFF), (uint8_t)(programmer_pid >> 8),
        0x01, 0x00, 1, 2, 0, 1
    };
    if (write(fd, descriptor, 18) != 18) {
        std::cerr << "[MINIPRO-EMU] Error al escribir descriptor" << std::endl;
        return;
    }

    while (true) {
        int32_t packet_len = 0;
        if (!read_exactly(fd, (uint8_t*)&packet_len, 4)) {
            std::cout << "[MINIPRO-EMU] Cliente desconectado (EOF leyendo packet_len)" << std::endl;
            break;
        }

        uint8_t endpoint = 0;
        if (!read_exactly(fd, &endpoint, 1)) {
            std::cout << "[MINIPRO-EMU] EOF leyendo endpoint" << std::endl;
            break;
        }

        if (packet_len > 0) {
            // OUT transfer: read payload
            std::vector<uint8_t> buf(packet_len);
            if (!read_exactly(fd, buf.data(), packet_len)) {
                std::cout << "[MINIPRO-EMU] EOF leyendo datos OUT" << std::endl;
                break;
            }
            std::cout << "[MINIPRO-EMU] OUT Transfer: EP 0x" << std::hex << (int)endpoint 
                      << ", Longitud: " << std::dec << packet_len << " bytes. Datos: ";
            for (int i = 0; i < std::min((size_t)packet_len, (size_t)16); i++) {
                std::cout << "0x" << std::hex << (int)buf[i] << " ";
            }
            if (packet_len > 16) std::cout << "...";
            std::cout << std::dec << std::endl;

            // Procesar y almacenar escritura de datos entrelazados en EP2 y EP3
            if (endpoint == 0x01) {
                // Comando de control: decodificar comandos de lectura/escritura de memoria
                if (packet_len >= 1) {
                    last_control_command = buf[0];
                }
                if (packet_len >= 8) {
                    uint8_t cmd = buf[0];
                    if (cmd == 0x0D || cmd == 0x10 || cmd == 0x0C || cmd == 0x11) {
                        uint32_t addr = buf[4] | (buf[5] << 8) | (buf[6] << 16) | (buf[7] << 24);
                        std::cout << "[MINIPRO-EMU] Direccion fisica parseada: 0x" << std::hex << addr << std::dec << std::endl;
                        // TL866II+ usa EP2/EP3 entrelazados (bloques de 128 bytes);
                        // T48/T56/T76 usan solo EP2 con bloques contiguos de 64.
                        uint32_t stride = programmer_model == MODEL_TL866IIPLUS ? 128 : 64;
                        ep2_read_block = addr / stride;
                        ep3_read_block = addr / stride;
                        ep2_write_block = addr / stride;
                        ep3_write_block = addr / stride;

                        if ((cmd == 0x0C || cmd == 0x11) && packet_len > 8) {
                            uint16_t data_len = buf[2] | (buf[3] << 8);
                            size_t copy_len = std::min((size_t)data_len, (size_t)(packet_len - 8));
                            if (addr + copy_len <= virtual_memory.size()) {
                                memcpy(virtual_memory.data() + addr, buf.data() + 8, copy_len);
                            }
                        }
                    }
                }
            } else if (endpoint == 0x02) {
                for (size_t offset = 0; offset < (size_t)packet_len; offset += 64) {
                    size_t block = programmer_model == MODEL_TL866IIPLUS
                                           ? (2 * ep2_write_block)
                                           : ep2_write_block;
                    size_t dest_offset = block * 64;
                    if (dest_offset < virtual_memory.size()) {
                        size_t chunk = std::min((size_t)64, (size_t)(packet_len - offset));
                        memcpy(virtual_memory.data() + dest_offset, buf.data() + offset, chunk);
                    }
                    ep2_write_block++;
                }
            } else if (endpoint == 0x03) {
                for (size_t offset = 0; offset < (size_t)packet_len; offset += 64) {
                    size_t block = programmer_model == MODEL_TL866IIPLUS
                                           ? (2 * ep3_write_block + 1)
                                           : ep3_write_block;
                    size_t dest_offset = block * 64;
                    if (dest_offset < virtual_memory.size()) {
                        size_t chunk = std::min((size_t)64, (size_t)(packet_len - offset));
                        memcpy(virtual_memory.data() + dest_offset, buf.data() + offset, chunk);
                    }
                    ep3_write_block++;
                }
            }
        } else if (packet_len < 0) {
            // IN transfer request: write response of size abs(packet_len)
            int32_t req_len = -packet_len;
            std::cout << "[MINIPRO-EMU] IN Request: EP 0x" << std::hex << (int)endpoint 
                      << ", Solicitados: " << std::dec << req_len << " bytes" << std::endl;

            std::vector<uint8_t> resp(req_len, 0x00);
            
            // Si es endpoint 0x81 y req_len >= 80, respondemos con el status del programador
            if (endpoint == 0x81 && req_len >= 80) {
                resp[4] = programmer_firmware_minor();
                resp[5] = programmer_firmware_major();
                resp[6] = programmer_model;
                if (programmer_model == MODEL_TL866IIPLUS) {
                    memcpy(resp.data() + 8, "DEVCODE1", 8);
                    memcpy(resp.data() + 16, "SERIAL12345678901234", 20);
                    resp[40] = 4;  // HW version
                } else {
                    // T48 / T56 / T76: fecha, código y serie en otros offsets.
                    memcpy(resp.data() + 8, "2026-01-01", 10);
                    memcpy(resp.data() + 24, "DEVCODE1", 8);
                    memcpy(resp.data() + 32, "SERIAL12345678901234", 20);
                    uint32_t voltage = programmer_model == MODEL_T76 ? 5000 : 1522;
                    resp[56] = voltage & 0xFF;
                    resp[57] = (voltage >> 8) & 0xFF;
                    resp[58] = (voltage >> 16) & 0xFF;
                    resp[59] = (voltage >> 24) & 0xFF;
                    resp[60] = 0; // USB speed: 12 Mbps
                    resp[62] = 0; // External power (T56 only)
                }
                std::cout << "[MINIPRO-EMU] Enviando status mock del " << programmer_name() << std::endl;
            } else if (endpoint == 0x81 && (req_len == 16 || req_len == 32) &&
                       last_control_command == 0x37) {
                // Autodetección SPI (T48_AUTODETECT / TL866IIPLUS_AUTODETECT):
                // el ID JEDEC se devuelve en los bytes 2..4 (big endian).
                resp[2] = 0xEF; // Manufacturer ID (Winbond)
                resp[3] = 0x40; // Memory Type
                resp[4] = 0x14; // Capacity (8M-bit = 1MB)
                std::cout << "[MINIPRO-EMU] Enviando ID de autodetección mock (0xEF4014)" << std::endl;
            } else if (endpoint == 0x81 && (req_len == 6 || req_len == 32) &&
                       (last_control_command == 0x05 || last_control_command == 0x1D)) {
                // JEDEC ID del chip Winbond W25Q80 (SPI Flash, 1MB).
                // TL866II+ lee 6 bytes; T48/T56/T76 leen 32.
                resp[0] = 0x01; // ID Type: MP_ID_TYPE1
                resp[1] = 0x00;
                resp[2] = 0xEF; // Manufacturer ID (Winbond)
                resp[3] = 0x40; // Memory Type
                resp[4] = 0x14; // Capacity (8M-bit = 1MB)
                resp[5] = 0x00;
                std::cout << "[MINIPRO-EMU] Enviando JEDEC ID mock para W25Q80 (0xEF4014)" << std::endl;
            } else if (endpoint == 0x82) {
                // TL866II+: bloques pares entrelazados. T48/T56/T76: contiguos.
                for (size_t offset = 0; offset < (size_t)req_len; offset += 64) {
                    size_t block = programmer_model == MODEL_TL866IIPLUS
                                           ? (2 * ep2_read_block)
                                           : ep2_read_block;
                    size_t src_offset = block * 64;
                    if (src_offset < virtual_memory.size()) {
                        size_t chunk = std::min((size_t)64, (size_t)(req_len - offset));
                        memcpy(resp.data() + offset, virtual_memory.data() + src_offset, chunk);
                    } else {
                        memset(resp.data() + offset, 0xFF, std::min((size_t)64, (size_t)(req_len - offset)));
                    }
                    ep2_read_block++;
                }
            } else if (endpoint == 0x83) {
                // TL866II+: bloques impares entrelazados. T48/T56/T76: contiguos.
                for (size_t offset = 0; offset < (size_t)req_len; offset += 64) {
                    size_t block = programmer_model == MODEL_TL866IIPLUS
                                           ? (2 * ep3_read_block + 1)
                                           : ep3_read_block;
                    size_t src_offset = block * 64;
                    if (src_offset < virtual_memory.size()) {
                        size_t chunk = std::min((size_t)64, (size_t)(req_len - offset));
                        memcpy(resp.data() + offset, virtual_memory.data() + src_offset, chunk);
                    } else {
                        memset(resp.data() + offset, 0xFF, std::min((size_t)64, (size_t)(req_len - offset)));
                    }
                    ep3_read_block++;
                }
            } else if (endpoint == 0x81) {
                // Rellenar con 0x00 para evitar activar protecciones o flags de error en status/config
                std::fill(resp.begin(), resp.end(), 0x00);
            } else {
                std::fill(resp.begin(), resp.end(), 0xFF);
            }

            if (write(fd, resp.data(), req_len) != req_len) {
                std::cerr << "[MINIPRO-EMU] Error al escribir respuesta IN" << std::endl;
                break;
            }
        }
    }
}

// Servidor TL866II+ en UNIX Socket para modo background
void minipro_socket_server(const std::string& path) {
    unlink(path.c_str());
    int server_fd = socket(AF_UNIX, SOCK_STREAM, 0);
    if (server_fd < 0) {
        std::cerr << "[MINIPRO-EMU] Error socket UNIX" << std::endl;
        return;
    }

    struct sockaddr_un addr;
    memset(&addr, 0, sizeof(addr));
    addr.sun_family = AF_UNIX;
    strncpy(addr.sun_path, path.c_str(), sizeof(addr.sun_path)-1);

    if (bind(server_fd, (struct sockaddr*)&addr, sizeof(addr)) < 0) {
        std::cerr << "[MINIPRO-EMU] Error bind UNIX socket" << std::endl;
        close(server_fd);
        return;
    }

    if (listen(server_fd, 5) < 0) {
        std::cerr << "[MINIPRO-EMU] Error listen UNIX socket" << std::endl;
        close(server_fd);
        return;
    }

    std::cout << "[MINIPRO-EMU] Servidor UNIX escuchando en " << path << std::endl;

    while (true) {
        int client_fd = accept(server_fd, nullptr, nullptr);
        if (client_fd >= 0) {
            handle_minipro_client(client_fd);
            close(client_fd);
        }
    }
    close(server_fd);
}

int main(int argc, char* argv[]) {
    std::string socket_path = "./minipro_socket";
    int minipro_arg_idx = -1;
    std::string fill_pattern = "";
    std::string programmer_arg = "";

    for (int i = 1; i < argc; i++) {
        std::string arg = argv[i];
        if (arg == "--socket" && i + 1 < argc) {
            socket_path = argv[++i];
        } else if (arg == "--flash" && i + 1 < argc) {
            flash_file_path = argv[++i];
        } else if (arg == "--save" && i + 1 < argc) {
            save_file_path = argv[++i];
        } else if (arg == "--fill" && i + 1 < argc) {
            fill_pattern = argv[++i];
        } else if (arg == "--programmer" && i + 1 < argc) {
            programmer_arg = argv[++i];
        } else if (strstr(argv[i], "minipro") != nullptr) {
            minipro_arg_idx = i;
            break;
        } else if (arg == "-h" || arg == "--help") {
            std::cout << "Uso: emulador_minipro [OPCIONES]\n"
                      << "  --socket RUTA    Escucha en socket UNIX en RUTA\n"
                      << "  --flash ARCHIVO  Carga el contenido inicial de la memoria desde ARCHIVO\n"
                      << "  --save ARCHIVO   Guarda el contenido de la memoria al salir en ARCHIVO\n"
                      << "  --fill PATRON    Rellena la memoria: empty (0xFF), count (0..255), random\n"
                      << "  --programmer M   Emula el programador: tl866ii (defecto), t48, t56, t76\n"
                      << "  minipro [ARGS]   Ejecuta minipro en modo auto-contenido\n";
            return 0;
        }
    }

    if (!programmer_arg.empty()) {
        std::string p = programmer_arg;
        std::transform(p.begin(), p.end(), p.begin(),
                       [](unsigned char c) { return (char)std::tolower(c); });
        if (p == "t48") {
            programmer_model = MODEL_T48;
            programmer_vid = 0xa466;
            programmer_pid = 0x0a53;
        } else if (p == "t56") {
            programmer_model = MODEL_T56;
            programmer_vid = 0xa466;
            programmer_pid = 0x0a53;
        } else if (p == "t76") {
            programmer_model = MODEL_T76;
            programmer_vid = 0xa466;
            programmer_pid = 0x1a86;
        } else if (p == "tl866ii" || p == "tl866ii+" || p == "tl866iiplus") {
            programmer_model = MODEL_TL866IIPLUS;
            programmer_vid = 0xa466;
            programmer_pid = 0x0a53;
        } else {
            std::cerr << "Programador no soportado: " << programmer_arg
                      << " (usa tl866ii, t48, t56 o t76)" << std::endl;
            return 1;
        }
        std::cout << "[MINIPRO-EMU] Programador emulado: " << programmer_name()
                  << " (VID:PID "
                  << std::hex << programmer_vid << ":" << programmer_pid
                  << std::dec << ")" << std::endl;
    }

    if (!fill_pattern.empty()) {
        fill_memory(fill_pattern);
    }
    if (!flash_file_path.empty()) {
        load_memory(flash_file_path);
    }

    if (minipro_arg_idx != -1) {
        std::cout << "[MINIPRO-EMU] Modo auto-contenido detectado. Lanzando subproceso..." << std::endl;
        int sv[2];
        if (socketpair(AF_UNIX, SOCK_STREAM, 0, sv) < 0) {
            std::cerr << "Error socketpair: " << strerror(errno) << std::endl;
            return 1;
        }

        pid_t pid = fork();
        if (pid == 0) {
            // Proceso hijo (ejecutará minipro)
            close(sv[0]);
            dup2(sv[1], 99); // Duplicar socket a FD 99
            close(sv[1]);

            setenv("ANDROID_USB_FD", "99", 1);
            
            // Construir argv para execvp
            std::vector<char*> child_argv;
            for (int i = minipro_arg_idx; i < argc; i++) {
                child_argv.push_back(argv[i]);
            }
            child_argv.push_back(nullptr);

            execvp(child_argv[0], child_argv.data());
            std::cerr << "Error en execvp: " << strerror(errno) << std::endl;
            exit(1);
        } else if (pid > 0) {
            // Proceso padre (corre emulador TL866II+)
            close(sv[1]);
            handle_minipro_client(sv[0]);
            close(sv[0]);

            int status;
            waitpid(pid, &status, 0);
            std::cout << "[MINIPRO-EMU] Subproceso minipro finalizado." << std::endl;

            if (!save_file_path.empty()) {
                save_memory(save_file_path);
            }
            return 0;
        }
    } else {
        minipro_socket_server(socket_path);
    }

    return 0;
}
