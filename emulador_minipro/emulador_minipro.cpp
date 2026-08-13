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

// Memoria virtual del programador (1MB para simular SPI y EEPROM)
std::vector<uint8_t> virtual_memory(1048576, 0xFF);
std::string flash_file_path = "";
std::string save_file_path = "";

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
    uint8_t tl866ii_descriptor[18] = {
        18, 1, 0x00, 0x02, 0, 0, 0, 64, 
        0x66, 0xa4, 0x53, 0x0a, 0x01, 0x00, 
        1, 2, 0, 1
    };
    if (write(fd, tl866ii_descriptor, 18) != 18) {
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
                if (packet_len >= 8) {
                    uint8_t cmd = buf[0];
                    if (cmd == 0x0D || cmd == 0x10 || cmd == 0x0C || cmd == 0x11) {
                        uint32_t addr = buf[4] | (buf[5] << 8) | (buf[6] << 16) | (buf[7] << 24);
                        std::cout << "[MINIPRO-EMU] Direccion fisica parseada: 0x" << std::hex << addr << std::dec << std::endl;
                        ep2_read_block = addr / 128;
                        ep3_read_block = addr / 128;
                        ep2_write_block = addr / 128;
                        ep3_write_block = addr / 128;
                    }
                }
            } else if (endpoint == 0x02) {
                for (size_t offset = 0; offset < (size_t)packet_len; offset += 64) {
                    size_t dest_offset = (2 * ep2_write_block) * 64;
                    if (dest_offset < virtual_memory.size()) {
                        size_t chunk = std::min((size_t)64, (size_t)(packet_len - offset));
                        memcpy(virtual_memory.data() + dest_offset, buf.data() + offset, chunk);
                    }
                    ep2_write_block++;
                }
            } else if (endpoint == 0x03) {
                for (size_t offset = 0; offset < (size_t)packet_len; offset += 64) {
                    size_t dest_offset = (2 * ep3_write_block + 1) * 64;
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
            
            // Si es endpoint 0x81 y req_len >= 80, respondemos con el status del TL866II+
            if (endpoint == 0x81 && req_len >= 80) {
                resp[4] = 143; // minor version
                resp[5] = 2;   // major version
                resp[6] = 5;   // device type (MP_TL866IIPLUS)
                memcpy(resp.data() + 8, "DEVCODE1", 8);
                memcpy(resp.data() + 16, "SERIAL12345678901234", 20);
                resp[40] = 4;  // HW version
                std::cout << "[MINIPRO-EMU] Enviando status mock del TL866II+" << std::endl;
            } else if (endpoint == 0x81 && req_len == 6) {
                // JEDEC ID del chip Winbond W25Q80 (SPI Flash, 1MB)
                resp[0] = 0x01; // ID Type: MP_ID_TYPE1
                resp[1] = 0x00;
                resp[2] = 0xEF; // Manufacturer ID (Winbond)
                resp[3] = 0x40; // Memory Type
                resp[4] = 0x14; // Capacity (8M-bit = 1MB)
                resp[5] = 0x00;
                std::cout << "[MINIPRO-EMU] Enviando JEDEC ID mock para W25Q80 (0xEF4014)" << std::endl;
            } else if (endpoint == 0x82) {
                // Lectura entrelazada de bloques pares
                for (size_t offset = 0; offset < (size_t)req_len; offset += 64) {
                    size_t src_offset = (2 * ep2_read_block) * 64;
                    if (src_offset < virtual_memory.size()) {
                        size_t chunk = std::min((size_t)64, (size_t)(req_len - offset));
                        memcpy(resp.data() + offset, virtual_memory.data() + src_offset, chunk);
                    } else {
                        memset(resp.data() + offset, 0xFF, std::min((size_t)64, (size_t)(req_len - offset)));
                    }
                    ep2_read_block++;
                }
            } else if (endpoint == 0x83) {
                // Lectura entrelazada de bloques impares
                for (size_t offset = 0; offset < (size_t)req_len; offset += 64) {
                    size_t src_offset = (2 * ep3_read_block + 1) * 64;
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
        } else if (strstr(argv[i], "minipro") != nullptr) {
            minipro_arg_idx = i;
            break;
        } else if (arg == "-h" || arg == "--help") {
            std::cout << "Uso: emulador_minipro [OPCIONES]\n"
                      << "  --socket RUTA    Escucha en socket UNIX en RUTA\n"
                      << "  --flash ARCHIVO  Carga el contenido inicial de la memoria desde ARCHIVO\n"
                      << "  --save ARCHIVO   Guarda el contenido de la memoria al salir en ARCHIVO\n"
                      << "  --fill PATRON    Rellena la memoria: empty (0xFF), count (0..255), random\n"
                      << "  minipro [ARGS]   Ejecuta minipro en modo auto-contenido\n";
            return 0;
        }
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
