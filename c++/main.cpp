/*
NOTICE: You may compile/execute this file via the included 
build.bat. However, to run said batch file, you will need to 
install MinGW-w64 and put it on the path.

DISCLAIMER: IT IS NOT MY FAULT IF YOU FAIL TO FOLLOW THE
ABOVE INSTRUCTIONS. YOU SHOULD KNOW BETTER.

I AM NOT COMPLETELY SURE IF THIS WORKS WITH MSVC OR CLANG.
YOU WILL HAVE TO DEAL WITH THAT YOURSELF.

-superminer
*/

#include <iostream>
#include <fstream>

#include <string>
#include <array>
#include <vector>

#include <cstring>
#include <cstdlib>

#include <windows.h>
#include <tchar.h>

// Type macros
#define u64 uint64_t
#define u8 uint8_t
#define u16 uint16_t
#define wcstr wchar_t*
#define cstr char*

// Function calls
#define call_void_fn(handle, name) ((void (*)(void))GetProcAddress(handle, name))()
#define assert(cond, msg, code) \
  if (!(cond))                  \
  {                             \
    cout << msg << endl;        \
    exit(code);                 \
  }

// Constant macros
#define null NULL

#define OBJ_SIZE 1392
#define OBJ_START_OFFSET 160
#define OBJ_COPIABLE_SIZE (OBJ_SIZE - OBJ_START_OFFSET)

using std::cout, std::cerr, std::endl, std::ifstream, std::ofstream;
using std::string, std::vector, std::array;

// separate sm64 functions from bruteforcer
namespace libsm64 {
  struct MemRegion {
    u64 address;
    u64 size;
  };

  enum Version {
    VERSION_US,
    VERSION_JP
  };

  const struct MemRegion SEGMENTS[2][2] = {
      {{1302528, 2836576},
       {14045184, 4897408}},
      {{1294336, 2406112},
       {13594624, 4897632}},
  };

  struct InputFrame {
    u16 buttons;
    u8 stick_x, stick_y;
  };

  using Slot = std::array<vector<u8>, 2>;
  using M64 = vector<InputFrame>;

  class Game {
  private:
    Version m_version;
    HMODULE m_dll;
    const MemRegion* m_regions;

  public:
    Game(Version version, cstr dll_path) {
      m_version = version;
      cout << "received " << dll_path << endl;
      m_dll = LoadLibraryA(dll_path);

      if (m_dll == NULL) {
        unsigned int lastError = GetLastError();
        cerr << "Last error is " << lastError << endl;
        exit(-2);
      }
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
        vector<u8>& buffer = slot[i];
        memmove(&buffer[0], m_dll + region.address, region.size);
      }
    }

    void load_slot(Slot slot) {
      for (int i = 0; i < 2; i++) {
        MemRegion region = m_regions[i];
        vector<u8>& buffer = slot[i];
        memmove(&buffer[0], m_dll + region.address, region.size);
      }
    }

    template <typename T = void>
    // Locates a symbol within libsm64 and optionally applies an offset.
    T* addr(cstr name, u64 off = 0) {
      // cast to byte pointer, apply offset, then cast to T
      return reinterpret_cast<T*>(
        reinterpret_cast<u8*>(GetProcAddress(m_dll, name)) + off);
    }

    void set_input(InputFrame input) {
      *addr<u16>("gControllerPads", 0) = input.buttons;
      *addr<u8>("gControllerPads", 2) = input.stick_x;
      *addr<u8>("gControllerPads", 3) = input.stick_y;
    }
  };

  M64 load_m64(cstr path) {
    M64 result = {};

    ifstream file;
    file.open(path);
    file.seekg(0x400);

    u8 bytes[4];

    while (!file.eof()) {
      file.read((char*)bytes, 4);

      u16 buttons = bytes[0];
      buttons <<= 8;
      buttons |= bytes[1];

      result.push_back({ buttons, bytes[2], bytes[3] });
    }
    file.close();
    return result;
  }
  // Copies an object from src to dst.
  void copy_object(Game g, int src, int dst) {
    void* aPtr = g.addr("gObjectPool", (src * OBJ_SIZE) + OBJ_START_OFFSET);
    void* bPtr = g.addr("gObjectPool", (dst * OBJ_SIZE) + OBJ_START_OFFSET);
    memcpy(bPtr, aPtr, OBJ_COPIABLE_SIZE);
  }

  void dump(Slot slot) {
    ofstream dump = ofstream();
    dump.open("dump2.bin");
    for (int i = 0; i < 2; i++) {
      dump.write((char*)slot[i].data(), (u64)slot[i].size());
    }
  }
};

using libsm64::Game, libsm64::Version;

const int BULLY_SLOTS[] = {
  // list(range(24)), unrolled
  1, 2, 3, 4, 5, 6, 7, 8,
  9, 10, 11, 12, 13, 14, 15,
  16, 17, 18, 19, 20, 21, 22, 23,
  24,
  // the other stuff
  26, 27, 28, 29, 30, 32,
  34,
  35, 37, 38, 39, 40, 42,
  48, 49,
  50,
  51,
  52, 53, 54, 55, 56, 57, 58, 60, 61, 63, 64, 65, 66, 67, 87,
  90, 91, 92, 93, 95, 96, 98, 99, 105, 106, 107 };

int main(int argc, cstr argv[]) {
  assert(argc == 2, "Please specify param 1: Path to libsm64", 64);

  cerr << "wafel path is " << argv[1] << endl;

  Game game = Game(Version::VERSION_JP, argv[1]);

  auto m64 = libsm64::load_m64("..\\shared\\1Key_4_21_13_Padded.m64");
  auto backup = game.alloc_slot();

  cerr << "Running M64..." << endl;
  for (int frame = 0; frame < m64.size(); frame++) {
    game.set_input(m64[frame]);
    game.advance();

    u16* star_count = game.addr<u16>("gMarioStates", 230);
    if ((frame % 1000) == 0) {
      cerr << "Frame " << frame << ", " << *star_count << " stars collected" << endl;
    }

    if (frame == 3285) {
      cerr << endl;
      // deactivate every object that doesn't need to be active
      for (int i = 0; i < 108; i++) {
        switch (i) {
          case 27:
          case 83:
          case 84:
          case 89: {
            // do nothing
          } break;
          default: {
            //deactivate this object
            u16* activeFlag = game.addr<u16>("gObjectPool", i * OBJ_SIZE + 180);
            *activeFlag &= (u16)0xFFFE;
            cerr << "Deactivated slot " + i << endl;
          } break;
        }
      }

      for (int i: BULLY_SLOTS) {
        libsm64::copy_object(game, 27, i);
        cerr << "Copied bully to slot " << i << endl;
      }
      game.save_slot(backup);
      cerr << "Saved state" << endl;
      libsm64::dump(backup);
      cerr << "Dumped" << endl;
      break;
    }
  }

  return 0;
}