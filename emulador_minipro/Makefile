CXX = g++
CXXFLAGS = -O3 -Wall -std=c++11 -pthread
TARGET = emulador_minipro

all: $(TARGET)

$(TARGET): emulador_minipro.cpp
	$(CXX) $(CXXFLAGS) -o $(TARGET) emulador_minipro.cpp

clean:
	rm -f $(TARGET)
