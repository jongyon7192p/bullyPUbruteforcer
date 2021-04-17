#!/usr/bin/env python
# coding: utf-8

# In[1]:


from typing import *
import ctypes as C
import struct

SEGMENTS = {
    'us': [
      { 'name': '.data', 'virtual_address': 1302528, 'virtual_size': 2836576 },
      { 'name': '.bss', 'virtual_address': 14045184, 'virtual_size': 4897408 },
    ],
    'jp': [
      { 'name': '.data', 'virtual_address': 1294336, 'virtual_size': 2406112 },
      { 'name': '.bss', 'virtual_address': 13594624, 'virtual_size': 4897632 },
    ],
}

class Game:
    def __init__(self, version, dll_path):
        self.version = version
        self.dll = C.cdll.LoadLibrary(dll_path)
        self.dll.sm64_init()
        self.segments = SEGMENTS[version]

    def advance_frame(self):
        self.dll.sm64_update()

    def alloc_slot(self):
        buffers = []
        for segment in self.segments:
            buffers.append(C.create_string_buffer(segment['virtual_size']))
        return buffers

    def save_state(self, slot):
        for segment, buffer in zip(self.segments, slot):
            C.memmove(buffer, self.dll._handle + segment['virtual_address'], segment['virtual_size'])

    def load_state(self, slot):
        for segment, buffer in zip(self.segments, slot):
            C.memmove(self.dll._handle + segment['virtual_address'], buffer, segment['virtual_size'])

    def addr(self, symbol):
        return C.addressof(C.c_uint32.in_dll(self.dll, symbol))

def ptr(addr, type):
    return C.cast(addr, C.POINTER(type))

def load_m64(filename):
    frames = []
    with open(filename, 'rb') as f:
        f.seek(0x400)
        while True:
            try:
                buttons = struct.unpack('>H', f.read(2))[0]
                stick_x = struct.unpack('=b', f.read(1))[0]
                stick_y = struct.unpack('=b', f.read(1))[0]
            except struct.error:
                break
            frames.append((buttons, stick_x, stick_y))
    return frames

def set_inputs(game, inputs):
    buttons, stick_x, stick_y = inputs
    ptr(game.addr('gControllerPads') + 0, C.c_uint16)[0] = buttons
    ptr(game.addr('gControllerPads') + 2, C.c_int8)[0] = stick_x
    ptr(game.addr('gControllerPads') + 3, C.c_int8)[0] = stick_y

################################################################################

game = Game('jp', 'C:/LW-Program-Files/SM64 TAS/WAFEL/libsm64/sm64_jp.dll')
m64 = load_m64('../shared/1Key_4_21_13_Padded.m64')

backup = game.alloc_slot()
backupFrame = None

for frame in range(len(m64)):
    set_inputs(game, m64[frame])
    game.advance_frame()

    num_stars = ptr(game.addr('gMarioStates') + 230, C.c_int16)[0]
    
    if (frame % 1000 == 0): 
        print("Frame %05d stars %02d" % (frame, num_stars))
        
    if (frame == 3286):
        for obj in range(108):
            #don't deactivate the bully or the tilting pyramid platforms
            if obj in [27, 83, 84]:
                continue
            #seems to be either 48 or 180
            activeFlag = ptr(game.addr('gObjectPool') + obj*1392 + 180, C.c_short)
            #print(activeFlag[0])
            activeFlag[0] = activeFlag[0] & 0xFFFE
        game.save_state(backup)
        backupFrame = frame + 1
        break


# In[2]:


# Pointers and definitions.
marioX = ptr(game.addr('gMarioStates') + 60, C.c_float)
marioY = ptr(game.addr('gMarioStates') + 64, C.c_float)
marioZ = ptr(game.addr('gMarioStates') + 68, C.c_float)

bullyX    = ptr(game.addr('gObjectPool') + 27 * 1392 + 240, C.c_float)
bullyY    = ptr(game.addr('gObjectPool') + 27 * 1392 + 244, C.c_float)
bullyZ    = ptr(game.addr('gObjectPool') + 27 * 1392 + 248, C.c_float)
bullyHSpd = ptr(game.addr('gObjectPool') + 27 * 1392 + 264, C.c_float)
bullyYaw1 = ptr(game.addr('gObjectPool') + 27 * 1392 + 280, C.c_uint16)
bullyYaw2 = ptr(game.addr('gObjectPool') + 27 * 1392 + 292, C.c_uint16)

startBullyPos = (-2236, -2950, -566)

initialYaw = 355


# In[3]:


game.load_state(backup)
print(bullyX[0])
print(bullyY[0])
print(bullyZ[0])
print(bullyHSpd[0])
print(bullyYaw1[0])
print(bullyYaw2[0])


# In[4]:


import numpy as np
import numpy.random as random


# In[5]:


def f2i(x):
  return struct.unpack('>l', struct.pack('>f', x))[0]

def i2f(x):
  return struct.unpack('>f', struct.pack('>i', x))[0]


# In[ ]:


#good_results = []
positions = set()

startSpeed = 20000000.0
for i in range(f2i(startSpeed), f2i(900000000.0)): 
    bullySpeed = i2f(i)
    #if i % 512 == 0:
        #print(i)
        #print(bullySpeed)
        
    for initialYaw in range(65535):
        #initialYaw=16385+2048*j+random.randint(0,2048)
        #if initialYaw>32768:
        #    initialYaw+=16383

        game.load_state(backup)
        bullyX[0] = startBullyPos[0]
        bullyY[0] = startBullyPos[1]
        bullyZ[0] = startBullyPos[2]

        bullyHSpd[0] = bullySpeed
        bullyYaw1[0] = initialYaw
        bullyYaw2[0] = initialYaw

        for iter_frame in range(26):
            marioX[0] = -1945
            marioY[0] = -2918
            marioZ[0] = -715
            game.advance_frame()
            newBullyPos = (bullyX[0], bullyY[0], bullyZ[0])

            dist = ((newBullyPos[0] +1720)**2 +
                    (newBullyPos[2] +460)**2)**.5

            if dist < 300:
                #good_results.append((bullySpeed, newBullyPos))
                #print(good_results)
                if newBullyPos not in positions:
                    print(str((startBullyPos[0], startBullyPos[1], startBullyPos[2], bullySpeed,
                                 initialYaw, iter_frame + 1, newBullyPos, dist, dist/bullySpeed, bullyYaw1[0])))
                positions.add(newBullyPos)
                with open("results_040521.txt", "a") as f:
                    f.write(str((startBullyPos[0], startBullyPos[1], startBullyPos[2], bullySpeed,
                                 initialYaw, iter_frame + 1, newBullyPos, dist, dist/bullySpeed, bullyYaw1[0])))
                    f.write("\n")


# In[ ]:




