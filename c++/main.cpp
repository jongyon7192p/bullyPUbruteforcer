/*
NOTICE: You may compile/execute this file via build.bat.
However, to run said batch file, you will need to install MinGW.
In addition, you will also need to put MinGW on PATH.
*/

#include <iostream>
#include <fstream>

#include <string>
#include <array>
#include <vector>

#include <string.h>

#include <windows.h>

// Type macros
#define u64 uint64_t
#define u8 uint8_t
#define u16 uint16_t
#define wcstr const wchar_t*
#define cstr const char*

// Function calls
#define call_void_fn(handle, name) ((void (*)(void))GetProcAddress(handle, name)())
#define assert(cond, msg, code) if (!(cond)) {cout << msg << endl; exit(code);} 

using std::cout, std::endl, std::ifstream;
using std::string, std::vector, std::array;

// separate sm64 functions from bruteforcer
namespace libsm64
{
  struct MemRegion {
    u64 address;
    u64 size;
  };

  enum Version {
    VERSION_US, VERSION_JP
  };

  const struct MemRegion SEGMENTS[2][2] = {
      {
        {1302528, 2836576},
        {14045184, 4897408}
      },
      {
        {1294336, 2406112},
        {13594624, 4897632}
      },
  };

  struct InputFrame {
    u16 buttons;
    u8 stick_x, stick_y;
  };

  class Game {
    private:
      Version m_version;
      HMODULE m_dll;
      const MemRegion* m_regions;
    public:
      using Slot = std::array<vector<u8>, 2>;

      Game(Version version, wcstr dll_path) {
        m_version = version;
        m_dll = LoadLibrary(dll_path);
        assert(m_dll != NULL, "DLL could not be loaded", 1);
        call_void_fn(m_dll, "sm64_init");
        m_regions = SEGMENTS[version];
      }

      ~Game() {
        FreeLibrary(m_dll);
      }

      void advance() {
        call_void_fn(m_dll, "sm64_update");
      }

      Slot alloc_slot() {
        Slot buffers = {};
        for (int i = 0; i < 2; i++) {
          buffers[i] = vector<u8>(m_regions[i].size);
        }
        return buffers;
      }

      void save_slot(Slot slot) {
        for (int i = 0; i < 2; i++) {
          MemRegion region = m_regions[i];
          vector<u8> &buffer = slot[i];
          memmove(&buffer[0], m_dll + region.address, region.size);
        }
      }

      void load_slot(Slot slot) {
        for (int i = 0; i < 2; i++) {
          MemRegion region = m_regions[i];
          vector<u8> &buffer = slot[i];
          memmove(&buffer[0], m_dll + region.address, region.size);
        }
      }

      template <typename T = void>
      T* addr(cstr name, u64 off = 0) {
        // cast to byte pointer, apply offset, then cast to T
        return reinterpret_cast<T*> (
          reinterpret_cast<u8*>(GetProcAddress(m_dll, name)) + off
        );
      }
      
      void set_input(InputFrame input) {
        *addr<u16>("gControllerPads", 0) = input.buttons;
        *addr<u8>("gControllerPads", 2) = input.stick_x;
        *addr<u8>("gControllerPads", 3) = input.stick_y;
      }
  };

  using M64 = vector<InputFrame>;

  M64 load_m64(cstr path) {
    M64 result = {};

    ifstream file;
    file.open(path);
    file.seekg(0x400);

    u8 bytes[4];

    while (!file.eof()) {
      file.read((char*) bytes, 4);

      u16 buttons = bytes[0];
      buttons <<= 8;
      buttons |= bytes[1];

      result.push_back({buttons, bytes[2], bytes[3]});
    }
    return result;
  }
};

using libsm64::Game;

int main() {
    
  return 0;
}