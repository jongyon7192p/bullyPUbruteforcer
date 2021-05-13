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
#include <iomanip>
#include <fstream>
#include <sstream>

#include <string>

#include <array>
#include <vector>
#include <bitset>
#include <tuple>

#include <limits>

#include <cstring>
#include <cmath>
#include <ctime>

#include <windows.h>
#include <tchar.h>

// Type macros
#define wcstr wchar_t*
#define cstr char*

// Function macros
#define call_void_fn(handle, name) ((void (*)(void))GetProcAddress(handle, name))()
#define assert(cond, msg, code) \
  if (!(cond))                  \
  {                             \
    cout << msg << endl;        \
    exit(code);                 \
  }
#define array_size(arr, type) (sizeof(arr) / sizeof(type))

// Constant macros
#define SPECIAL_FRAME 3285
#define TIME_NOW time(nullptr)

using std::cout, std::cerr, std::endl, std::fstream, std::ios_base, std::stringstream;
using std::string, std::vector, std::array, std::tuple;

// SM64-related functions
namespace libsm64 {
  struct MemRegion {
    const intptr_t address;
    const intptr_t size;
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
    uint16_t buttons;
    int8_t stick_x, stick_y;
  };
  using Slot = array<vector<uint8_t>, 2>;
  // Direct bindings to libsm64.
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
      Slot buffers = {
        vector<uint8_t>(m_regions[0].size),
        vector<uint8_t>(m_regions[1].size)
      };
      return buffers;
    }

    void save_state(Slot& slot) {
      uint8_t* const _dll = (uint8_t*)((void*)m_dll);
      for (int i = 0; i < 2; i++) {
        MemRegion segment = m_regions[i];
        std::vector<uint8_t>& buffer = slot[i];
        memmove(&buffer[0], _dll + segment.address, segment.size);
      }
    }

    void load_state(Slot& slot) {
      uint8_t* const _dll = (uint8_t*)((void*)m_dll);
      for (int i = 0; i < 2; i++) {
        MemRegion segment = m_regions[i];
        std::vector<uint8_t>& buffer = slot[i];
        memmove(_dll + segment.address, &buffer[0], segment.size);
      }
    }

    template <typename T = void>
    // Locates a symbol within libsm64 and optionally applies an offset.
    T* addr(string name, size_t off = 0) {
      // cast to byte pointer, apply offset, then cast to T
      return reinterpret_cast<T*>(
        reinterpret_cast<uint8_t*>(GetProcAddress(m_dll, name.c_str())) + off);
    }

    void set_input(InputFrame input) {
      *addr<uint16_t>("gControllerPads", 0) = input.buttons;
      *addr<uint8_t>("gControllerPads", 2) = input.stick_x;
      *addr<uint8_t>("gControllerPads", 3) = input.stick_y;
    }
  };
  // Essentially represents an M64.
  using M64 = vector<InputFrame>;
  // Loads an entire M64.
  M64 load_m64(string path) {
    M64 result = M64();
    fstream file = fstream(path, ios_base::in | ios_base::binary);
    //read length first (0x018, found in stroop)
    file.seekg(0x018, ios_base::beg);
    char inlen_data[4];
    file.read(inlen_data, 4);
    const uint32_t frames =
      ((uint8_t)inlen_data[0]) |
      ((uint8_t)inlen_data[1] << 8) |
      ((uint8_t)inlen_data[2] << 16) |
      ((uint8_t)inlen_data[3] << 24);
    //seek to data
    file.seekg(0x100, ios_base::beg);

    //allocated on the heap in case the stack can't hold that much
    char* next = new char[4 * frames];
    file.read(next, 4 * frames);
    for (uint16_t i = 0; i < frames; i++) {
      const uint16_t nextp = i * 4;
      const uint16_t buttons =
        ((uint8_t)next[nextp] << 8) |
        ((uint8_t)next[nextp + 1]);
      result.push_back(InputFrame{
        buttons,
        ((int8_t)next[nextp + 2]),
        ((int8_t)next[nextp + 3])
        });
    }
    // delete it after (ik this is bad c++ but i only needed an array, not a vector that's gonna reallocate itself)
    delete[] next;
    return result;
  }

  void copy_object(Game& game, size_t src, size_t dst) {
    uint8_t* const src_ptr = game.addr<uint8_t>("gObjectPool", (src * 1392) + 160);
    uint8_t* const dst_ptr = game.addr<uint8_t>("gObjectPool", (dst * 1392) + 160);
    memmove(dst_ptr, src_ptr, 1232);
  }

  void dump(Slot& slot) {
    fstream dump = fstream(
      "dump.bin",
      ios_base::out | ios_base::binary
    );
    for (int i = 0; i < 2; i++) {
      dump.write((char*)slot[i].data(), (uint64_t)slot[i].size());
    }
  }
};

using libsm64::Game, libsm64::Version, libsm64::M64;

/*
Bruteforcer Program here
*/

//Reinterprets the bytes of a uint32_t as a float.
float i2f(const uint32_t i) {
  return (*(float*)&i);
}
//Reinterprets the bytes of a float as a uint32_t.
uint32_t f2i(const float i) {
  return (*(uint32_t*)&i);
}

//Extremely basic 3D vector
struct vec3f {
public:
  const float x, y, z;
  vec3f(float x, float y, float z) : x(x), y(y), z(z) {}

  double hdist(vec3f other) {
    vec3f _this = (*this);
    double dx = _this.x - other.x;
    double dz = _this.z - other.z;
    return sqrt(dx * dx + dz * dz);
  }

  // Checks whether these two vectors are exactly equal.
  bool operator==(const vec3f& other) {
    vec3f _this = (*this);
    return (_this.x == other.x) && (_this.y == other.y) && (_this.z == other.z);
  }

  // Checks whether these two vectors are not exactly equal.
  bool operator!=(const vec3f& other) {
    vec3f _this = (*this);
    return (_this.x != other.x) || (_this.y != other.y) || (_this.z != other.z);
  }
};
// custom formatter.
std::ostream& operator<<(std::ostream& stream, const vec3f& vector) {
  return stream << "(" << vector.x << ", " << vector.y << ", " << vector.z << ")";
}

// Pointer to a bully's values
struct Bully {
  float* const x;
  float* const y;
  float* const z;

  float* const h_speed;
  uint16_t* const yaw_1;
  uint16_t* const yaw_2;

public:
  Bully(Game& game, int slot) :
    x(game.addr<float>("gObjectPool", (slot * 1392) + 240)),
    y(game.addr<float>("gObjectPool", (slot * 1392) + 244)),
    z(game.addr<float>("gObjectPool", (slot * 1392) + 248)),
    h_speed(game.addr<float>("gObjectPool", (slot * 1392) + 264)),
    yaw_1(game.addr<uint16_t>("gObjectPool", (slot * 1392) + 280)),
    yaw_2(game.addr<uint16_t>("gObjectPool", (slot * 1392) + 292)) {}

  vec3f pos() {
    return vec3f(*x, *y, *z);
  }

  void pos(vec3f val) {
    *x = val.x;
    *y = val.y;
    *z = val.z;
  }
};

// Pointers to Mario's values (that we need)
struct Mario {
  float* const x;
  float* const y;
  float* const z;

public:
  Mario(Game& game) :
    x(game.addr<float>("gMarioStates", 60)),
    y(game.addr<float>("gMarioStates", 64)),
    z(game.addr<float>("gMarioStates", 68)) {}

  vec3f pos() {
    return vec3f(*x, *y, *z);
  }

  void pos(vec3f val) {
    *x = val.x;
    *y = val.y;
    *z = val.z;
  }
};

// a speed and angle that can be applied to a bully
struct BullyState {
  float speed;
  uint16_t angle;
};

// Iterator for multibully's sake
struct StateIterator {
public:
  StateIterator(float min_speed) {
    //this is how you do it.
    speed = f2i(min_speed);
    angle = 0;
  }

  BullyState next() {
    BullyState result = BullyState{
      i2f(speed), angle
    };

    const uint16_t last_angle = angle;

    if ((angle % 16) == 0)
      angle += 1;
    else
      angle += 15;
    // detect overflow and increment speed
    if (angle < last_angle)
      speed++;
    return result;
  }

  uint32_t speed;
  uint16_t angle;
};

// All the slots that can be replaced with bullies.
const uint16_t BULLY_SLOTS[] = {
  // list(range(24)), unrolled
  0, 1, 2, 3, 4, 5, 6, 7, 8,
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

/*
CONSTANTS FOR BRUTEFORCING
*/

const vec3f BULLY_START_POS = vec3f(-2236, -2950, -566);
const vec3f MARIO_HACK_POS = vec3f(-1945, -2918, -715);
const vec3f TARGET_POS = vec3f(-1720, -2910, -460);

const uint32_t MAX_FRAMES = 25;
const float MAX_DIST = 1000.0f;

const float MIN_SPEED = 4052000.0f;
const float MAX_SPEED = 5000000.0f;


//generates an ansi code from a list of codes
string ansi(std::initializer_list<uint8_t> codes) {
  stringstream ss = stringstream();
  ss << "\u001B[";
  for (uint16_t code: codes) {
    ss << code << ";";
  }
  string s = ss.str();
  s[s.size() - 1] = 'm';
  return s;
}

int main(int argc, cstr argv[]) {
  // parse args
  assert(argc > 1, "Please specify param 1: Path to libsm64", 64);

  // setup libsm64
  cerr << "Loading libsm64 from " << argv[1] << endl;
  Game game = Game(Version::VERSION_JP, argv[1]);
  auto backup = game.alloc_slot();

  // Load M64
  cerr << "Loading 1-Key inputs" << endl;
  M64 m64 = libsm64::load_m64("..\\shared\\1Key_4_21_13_Padded.m64");
  // Step through m64
  for (size_t i = 0; i < m64.size(); i++) {
    game.set_input(m64[i]);
    game.advance();

    uint16_t* star_count = game.addr<uint16_t>("gMarioStates", 230);
    if ((i % 1000) == 0) {
      cerr << "Frame " << i << ", " << *star_count << " stars" << endl;
    }

    // On the *special* frame
    if (i == 3286) {
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
          uint16_t* activeFlag = game.addr<uint16_t>("gObjectPool", (i * 1392) + 180);
          *activeFlag &= (uint16_t)0xFFFE;
        } break;
        }
      }

      // Copy our favourite bully to 67 other objects within the course.
      for (uint16_t slot : BULLY_SLOTS) {
        libsm64::copy_object(game, 27, slot);
      }
      cerr << "All bullies set up" << endl;
      // Savestate. Everything is basically ready.
      game.save_state(backup);
      cerr << "Savestate saved" << endl;
      break;
    }
  }
  // Bruteforce constants

  Mario mario = Mario(game);
  vector<Bully> bullies = vector<Bully>();
  for (uint16_t i : BULLY_SLOTS) {
    bullies.push_back(Bully(game, i));
  }
  vector<BullyState> states = vector<BullyState>(bullies.size());

  cerr << "All pointers init'd" << endl;

  StateIterator iterator = StateIterator(MIN_SPEED);
  {
    fstream result = fstream("bully_results.txt", ios_base::out);
    auto then = TIME_NOW;

    while (i2f(iterator.speed) < MAX_SPEED) {
      game.load_state(backup);
      for (int i = 0; i < bullies.size(); i++) {
        BullyState next = iterator.next();
        if (next.angle == 0) {
          auto now = TIME_NOW;
          auto diff = now - then;
          auto past_speed = iterator.speed - 1;
          cerr << std::setprecision(std::numeric_limits<long double>::digits10 + 1);
          cerr << ansi({0, 32})<< "Float " << ansi({0, 96}) << i2f(past_speed) << 
            ansi({0, 32}) <<" in " << diff << " ms" << ansi({0}) << endl;
          then = now;
        }
        states[i] = next;
        Bully& bully = bullies[i];

        bully.pos(BULLY_START_POS);
        *(bully.h_speed) = next.speed;
        *(bully.yaw_1) = next.angle;
        *(bully.yaw_2) = next.angle;
      }

      for (uint32_t frame = 0; frame < MAX_FRAMES; frame++) {
        mario.pos(MARIO_HACK_POS);
        game.advance();

        for (uint32_t i = 0; i < bullies.size(); i++) {
          const double dist = bullies[i].pos().hdist(TARGET_POS);

          if (
            (dist <= MAX_DIST) &&
            (bullies[i].pos() != BULLY_START_POS)
            ) {
            //file out
            result << std::setprecision(std::numeric_limits<double>::digits10 + 1);
            result << L"Target: " << TARGET_POS << " Frame: " << frame << endl;
            result << L"Initial: │ Pos: " << BULLY_START_POS << " Speed: " << states[i].speed << " Angle: " << states[i].angle << endl;
            result << L"─────────┼───────────────────────────────────────────────────────────────────────────" << endl;
            result << L"Final:   │ Pos: " << bullies[i].pos() << " Speed: " << *(bullies[i].h_speed) << " Angle: " << *(bullies[i].yaw_1) << endl;
            result << L"─────────┴───────────────────────────────────────────────────────────────────────────" << endl;
            result << L"Distance to target: " << dist << endl << endl;
            // COUT
            cout << std::setprecision(std::numeric_limits<double>::digits10 + 1) << ansi({0, 93});
            cout << "Target: " << TARGET_POS << " Frame: " << frame << endl;
            cout << "Initial: │ Pos: " << BULLY_START_POS << " Speed: " << states[i].speed << " Angle: " << states[i].angle << endl;
            cout << "─────────┼───────────────────────────────────────────────────────────────────────────" << endl;
            cout << "Final:   │ Pos: " << bullies[i].pos() << " Speed: " << *(bullies[i].h_speed) << " Angle: " << *(bullies[i].yaw_1) << endl;
            cout << "─────────┴───────────────────────────────────────────────────────────────────────────" << endl;
            cout << "Distance to target: " << dist << endl << endl << ansi({0});
          }
        }
      }
    }
  }
  return 0;
}