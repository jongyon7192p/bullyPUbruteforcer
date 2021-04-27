#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <string.h>
#include <stdint.h>
#include <errno.h>
#include <windows.h>
#include <vector>
#include <array>

struct Segment {
  size_t virtualAddress, virtualSize;
};

enum Version { VERSION_JP, VERSION_US };

const Segment SEGMENTS[2][2] = {
  /* JP */ {
    { /* virtualAddress */  1302528, /* virtualSize */ 2836576 }, // .data
    { /* virtualAddress */ 14045184, /* virtualSize */ 4897408 }, // .bss
  },
  /* US */ {
    { /* virtualAddress */  1294336, /* virtualSize */ 2406112 }, // .data
    { /* virtualAddress */ 13594624, /* virtualSize */ 4897632 }, // .bss
  },
};

class Game {
  Version m_version;
  HMODULE m_dll;
  const Segment *m_segments;

public:
  using Slot = std::array<std::vector<uint8_t>, 2>;

  Game(Version version, const char *dllPath) {
    m_version = version;
    m_dll = LoadLibrary(dllPath);
    assert(m_dll != NULL && "can't load sm64 dll");
    ((void (*)(void))GetProcAddress(m_dll, "sm64_init"))();
    m_segments = SEGMENTS[version];
  }

  ~Game() { FreeLibrary(m_dll); }

  void advance_frame() {
    ((void (*)(void))GetProcAddress(m_dll, "sm64_update"))();
  }

  Slot alloc_slot() {
    Slot buffers = {};
    for (int i = 0; i < 2; i++) {
      buffers[i] = std::vector<uint8_t>(m_segments[i].virtualSize);
    }
    return buffers;
  }

  void save_state(Slot &slot) {
    for (int i = 0; i < 2; i++) {
      Segment segment = m_segments[i];
      std::vector<uint8_t> &buffer = slot[i];
      memmove(&buffer[0], m_dll + segment.virtualAddress, segment.virtualSize);
    }
  }

  void load_state(Slot &slot) {
    for (int i = 0; i < 2; i++) {
      Segment segment = m_segments[i];
      std::vector<uint8_t> &buffer = slot[i];
      memmove(m_dll + segment.virtualAddress, &buffer[0], segment.virtualSize);
    }
  }

  template <typename T = void>
  T *addr(const char *symbol, size_t byteOffset = 0) {
    return reinterpret_cast<T *>(
        reinterpret_cast<uint8_t *>(GetProcAddress(m_dll, symbol)) + byteOffset
    );
  }
};

struct InputFrame {
  uint16_t buttons;
  uint8_t stick_x, stick_y;
};

static auto load_m64(const char *filename) {
  std::vector<InputFrame> frames = {};
  
  FILE *f = fopen(filename, "rb");
  if (!f) {
    fprintf(stderr, "failed to load m64 '%s': %s\n", filename, strerror(errno));
  } else {
    fseek(f, 0x400, SEEK_SET);
    while (!feof(f) && !ferror(f)) {
      uint16_t buttons;
      buttons  = fgetc(f) << 8, buttons |= fgetc(f);
      uint8_t stick_x, stick_y;
      stick_x = fgetc(f), stick_y = fgetc(f);
      frames.push_back(InputFrame { buttons, stick_x, stick_y });
    }
    fclose(f);
  }

  return frames;
};

void set_inputs(Game &game, const InputFrame &inputs) {
  *game.addr<uint16_t>("gControllerPads", 0) = inputs.buttons;
  *game.addr<uint8_t>("gControllerPads", 2) = inputs.stick_x;
  *game.addr<uint8_t>("gControllerPads", 3) = inputs.stick_y;
}

int main() {
  auto game = Game(VERSION_US, "sm64_us.dll");
  const auto m64 = load_m64("120star.m64");
  auto backup = game.alloc_slot();

  for (int frame = 0; frame < (int)m64.size(); frame++) {
    set_inputs(game, m64[frame]);
    game.advance_frame();

    int num_stars = *game.addr<int16_t>("gMarioStates", 230);
    if (frame % 1000 == 0)
        printf("Frame %05d stars %02d\n", frame, num_stars);
  }
}
