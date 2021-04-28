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
#include <bitset>

#include <cstring>
#include <cstdlib>

#include <windows.h>
#include <tchar.h>

// Type macros
#define u8 uint8_t
#define u16 uint16_t
#define u32 uint32_t
#define u64 uint64_t
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
  // Iterator over an M64 file.
  struct M64 {
    //Constructs a new M64.
    M64(string file) {
      m_filename = string(file);

      m_file = ifstream();
      m_file.open(file);
      m_file.seekg(0x400);

      m_frame = 0;

      m_current = { 0, 0, 0 };
      this->operator++();
    }

    //Constructs a new M64 from another M64. (copy construction)
    M64(const M64& from) : m_filename(string(from.m_filename)), m_frame(from.m_frame) {
      m_file = ifstream();
      m_file.open(m_filename);

      if (from.m_frame) {
        m_file.seekg(0x400 + (from.m_frame * 4) - 4);
        this->operator++();
      }
      else {
        m_file.seekg(0x400);
      }

      this->operator++();
    }

    ~M64() {
      m_file.close();
    }

    M64& operator++() {
      u8 next[4];
      m_frame++;
      m_file.read((char*)next, 4);
      m_current.buttons = ((u16)next[0] << 8) | ((u16)next[1]);
      m_current.stick_x = next[2];
      m_current.stick_y = next[3];
      return *this;
    }

    M64& operator++(int) {
      M64 res = *this;
      ++(*this);
      return res;
    }

    const InputFrame& operator*() {
      return m_current;
    }

    const InputFrame* operator->() {
      return &m_current;
    }

    // Calls the copy constructor. (copy assignment)
    M64 operator=(M64& other) {
      return M64(other);
    }

    friend bool operator==(const M64& a, const M64& b) {
      return a.m_frame == b.m_frame;
    }

    friend bool operator!=(const M64& a, const M64& b) {
      return a.m_frame != b.m_frame;
    }

    u32 frame() { return m_frame; }
    bool eof() {
      return m_file.eof();
    }

  private:
    u32 m_frame;
    string m_filename;
    ifstream m_file;
    InputFrame m_current;
  };
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

using libsm64::Game, libsm64::Version, libsm64::M64;
using std::ios;

/*
Bruteforcer Program here
*/

// All the slots that can be replaced with bullies.
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

struct Bully {
  float* x;
  float* y;
  float* z;

  float* h_speed;
  u16* yaw_1;
  u16* yaw_2;

public:
  Bully(Game game, int slot) {
    x = game.addr<float>("gObjectPool", (slot * OBJ_SIZE) + 240);
    y = game.addr<float>("gObjectPool", (slot * OBJ_SIZE) + 244);
    z = game.addr<float>("gObjectPool", (slot * OBJ_SIZE) + 248);

    h_speed = game.addr<float>("gObjectPool", (slot * OBJ_SIZE) + 264);
    yaw_1 = game.addr<u16>("gObjectPool", (slot * OBJ_SIZE) + 280);
    yaw_1 = game.addr<u16>("gObjectPool", (slot * OBJ_SIZE) + 292);
  }
};

int main(int argc, cstr argv[]) {
  assert(argc == 2, "Please specify param 1: Path to libsm64", 64);

  cerr << "wafel path is " << argv[1] << endl;

  Game game = Game(Version::VERSION_JP, argv[1]);
  cerr << "libsm64 loaded" << endl;
  auto backup = game.alloc_slot();

  // Load M64
  cerr << "Savestate allocated, running M64..." << endl;
  M64 m64 = M64("..\\shared\\1Key_4_21_13_Padded.m64");
  // save cerr format
  ios* fmt_save = new ios(NULL);
  fmt_save->copyfmt(cerr);
  // Step through m64
  for (; !m64.eof(); m64++) {
    auto input = *m64;
    if ((m64.frame() % 60) == 0) {
      cerr.width(3);
      cerr << "Joystick: (" << (u16) input.stick_x << ", " << (u16) input.stick_y << ") ";
      cerr << "Buttons: " << std::bitset<16>(input.buttons) << endl;
      cerr.copyfmt(*fmt_save);
    }
    game.set_input(input);
    game.advance();

    u16* star_count = game.addr<u16>("gMarioStates", 230);
    // if ((m64.frame() % 1000) == 0) {
    //   cerr.width();
    //   cerr << "Frame " << m64.frame() << ", " << *star_count << " stars collected" << endl;
    // }
    cerr << "M64 frame " << m64.frame() << endl;

    // On the *special* frame
    if (m64.frame() == 3286) {
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
      // Copy our favourite bully to 67 other objects within the course
      for (int i : BULLY_SLOTS) {
        libsm64::copy_object(game, 27, i);
        cerr << "Copied bully to slot " << i << endl;
      }
      // Savestate, dump, and we're off!
      game.save_slot(backup);
      cerr << "Saved state" << endl;
      libsm64::dump(backup);
      cerr << "Dumped" << endl;
      break;
    }
  }


  return 0;
}