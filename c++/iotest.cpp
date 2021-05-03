#include <iostream>
#include <fstream>
#include <bitset>

#define STOP_FRAME 3286

using std::cout, std::cin, std::endl;
using std::ios_base, std::fstream;
using std::bitset;

/*
A short thing which reads through the M64.
*/
int main() {
  fstream file = fstream(
    "..\\shared\\1Key_4_21_13_Padded.m64",
    ios_base::in | ios_base::binary
  );

  file.seekg(0x018, ios_base::beg);
  char next[4];
  file.read(next, 4);
  uint32_t len = 
  ((uint8_t) next[0]) | 
  ((uint8_t) next[1] << 8) |
  ((uint8_t) next[2] << 16) |
  ((uint8_t) next[3] << 24);
  cout << "Length: " << len;

  cin.get();
  cin.get();

  file.seekg(0x400, ios_base::beg);

  char* bytes = new char[4 * STOP_FRAME];
  file.read(bytes, 4 * STOP_FRAME);
  for (int i = 0; i < (STOP_FRAME * 4); i += 4) {
    uint16_t buttons = ((uint16_t) bytes[i] << 8) | ((uint8_t) bytes[i + 1]);
    cout << "Buttons: " << bitset<16>(buttons) << " Joystick: (" << ((int16_t) bytes[i + 2]) << ", " << ((int16_t) bytes[i + 3]) << ")" << endl;
  }

  delete[] bytes;
}