
# To add a new cell, type '# %%'
# To add a new markdown cell, type '# %% [markdown]'
# %%
from typing import *
import os
import ctypes as C
import struct
import time
import numpy as np
import numpy.random as random

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
        try:
            os.remove("dump.bin")
        except OSError:
            pass
        
        for segment, buffer in zip(self.segments, slot):
            C.memmove(buffer, self.dll._handle + segment['virtual_address'], segment['virtual_size'])
            with open("dump.bin", "wb+") as dump:
                dump.write(buffer.raw)

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


# %%
#Hackish way to copy objects. I'm sure there is something better but
#I don't know what I'm doing
def copy_object(source_obj_num, to_obj_num):
    #If I copy the entirety of the object in memory, I get access violation
    #errors. Only copying part (but a larger part than this) can cause what
    #I think is an infinite loop. I don't think there's any documentation of
    #how Wafel lays out objects in memory, so this is basically guesswork.
    for i in range(40, 1392//4):
        to_ptr = ptr(game.addr('gObjectPool') + to_obj_num*1392 + i*4, C.c_float)
        source_ptr = ptr(game.addr('gObjectPool') + source_obj_num*1392 + i*4, C.c_float)
        to_ptr[0] = source_ptr[0]


# %%
################################################################################
game = Game('jp', 'C:/LW-Program-Files/SM64 TAS/Wafel/libsm64/sm64_jp.dll')
m64 = load_m64('C:/Users/jacky/OneDrive/Documents/GitHub/bullyPUbruteforcer/java/modules/bruteforcer/src/main/resources/assets/1Key_4_21_13_Padded.m64')

#Tyler figured out what kinds of objects are okay to go in here.
#In the end, turns out we can do up to 68 bullies.
bully_slot_order = [27] + list(range(25)) + [26, 28, 29, 30, 32]
bully_slot_order += [34,
                     35, 37, 38, 39, 40, 42,
                     48, 49,
                     50,
                     51,
                     52, 53, 54, 55, 56, 57, 58, 60, 61, 63, 64, 65, 66, 67, 87,
                     90, 91, 92, 93, 95, 96, 98, 99, 105, 106, 107]
#Allocate memory for a savestate
backup = game.alloc_slot()

for frame in range(len(m64)):
    #Run through the m64
    set_inputs(game, m64[frame])
    game.advance_frame()

    num_stars = ptr(game.addr('gMarioStates') + 230, C.c_int16)[0]
    
    if (frame % 1000 == 0): 
        print("Frame %05d stars %02d" % (frame, num_stars))
        
    #On the crucial frame...
    if (frame == 3285):
        #First, deactivate all of the objects but the bully, Mario, and the
        #tilting pyramid platforms. Trying to make things speedier and also avoid
        #spawners overwriting a bully with stuff
        for obj in range(108):
            if obj in [27, 89, 83, 84]:
                continue
            #seems to be either 48 or 180 to deactivate. I think it's 180
            active_flag = ptr(game.addr('gObjectPool') + obj*1392 + 180, C.c_short)
            active_flag[0] = active_flag[0] & 0xFFFE
        #Now for all of the extra bullies, copy memory over from our favorite bully
        #(in slot 27) to the target slot, overwriting whatever object was already there.
        for extra_bully in range(0, len(bully_slot_order)):
            source_num = 27
            to_num = bully_slot_order[extra_bully]
            copy_object(source_num, to_num)
        game.save_state(backup)
        break


# %%
# Pointers and definitions.
mario_x = ptr(game.addr('gMarioStates') + 60, C.c_float)
mario_y = ptr(game.addr('gMarioStates') + 64, C.c_float)
mario_z = ptr(game.addr('gMarioStates') + 68, C.c_float)

start_bully_pos = (-2236, -2950, -566)


# %%
def f2i(x):
  return struct.unpack('>l', struct.pack('>f', x))[0]

def i2f(x):
  return struct.unpack('>f', struct.pack('>i', x))[0]


# %%
#Function to get the next state we want to search. Only
#looks at numbers that are 0 or 1 mod 16
class StateIterator:
    def __init__(self, first_angle, first_speed_ind):
        self.next_angle = first_angle
        self.next_speed_ind = first_speed_ind
    def next_state(self):
        out = (self.next_angle, i2f(self.next_speed_ind))
        if self.next_angle % 16 == 0:
            self.next_angle += 1
        else:
            self.next_angle += 15
        if self.next_angle == 65536:
            self.next_angle = 0
            self.next_speed_ind += 1
        return out


# %%
bully_x_ptrs, bully_y_ptrs, bully_z_ptrs, bully_hspd_ptrs, bully_yaw_1_ptrs, bully_yaw_2_ptrs = [dict() for i in range(6)]

#Define some initial states here to start searching from.
#Will always search forward, incrementing angles frequently
#and floats whenever the next angle has been reached
first_angle = 0
#*************************************************************
#CHANGE first_speed_ind TO SEARCH DIFFERENT REGIONS
#*************************************************************
first_speed_ind = f2i(4052000.0)
#Specifically, it will start searching with speed i2f(first_speed_ind)
state_iterator = StateIterator(first_angle, first_speed_ind)

#Get all of the relevant pointers for each bully
for bully in range(len(bully_slot_order)):
    bully_x_ptrs[bully] = ptr(game.addr('gObjectPool') + bully_slot_order[bully]*1392 + 240, C.c_float)
    bully_y_ptrs[bully] = ptr(game.addr('gObjectPool') + bully_slot_order[bully]*1392 + 244, C.c_float)
    bully_z_ptrs[bully] = ptr(game.addr('gObjectPool') + bully_slot_order[bully]*1392 + 248, C.c_float)
    bully_hspd_ptrs[bully] = ptr(game.addr('gObjectPool') + bully_slot_order[bully]*1392 + 264, C.c_float)
    bully_yaw_1_ptrs[bully] = ptr(game.addr('gObjectPool') + bully_slot_order[bully]*1392 + 280, C.c_uint16)
    bully_yaw_2_ptrs[bully] = ptr(game.addr('gObjectPool') + bully_slot_order[bully]*1392 + 292, C.c_uint16)
        
while True:
    num_bullies = len(bully_slot_order)
    
    game.load_state(backup)
    
    #Keep track of each bully's initial conditions in case it returns a candidate
    bully_origins = [() for i in range(num_bullies)]
    
    for bully_num in range(num_bullies):
        bully_angle, bully_speed = state_iterator.next_state()
        bully_x_ptrs[bully_num][0] = start_bully_pos[0]
        bully_y_ptrs[bully_num][0] = start_bully_pos[1]
        bully_z_ptrs[bully_num][0] = start_bully_pos[2]
        bully_hspd_ptrs[bully_num][0] = bully_speed
        bully_yaw_1_ptrs[bully_num][0] = bully_angle
        bully_yaw_2_ptrs[bully_num][0] = bully_angle
        bully_origins[bully_num] = (bully_speed, bully_angle)
        
    for iter_frame in range(25):
        #Fix Mario to the center of the tilting platform to make it exist
        mario_x[0] = -1945
        mario_y[0] = -2918
        mario_z[0] = -715
        game.advance_frame()
        #Check the positions of each bully to see whether they are
        #candidates
        for bully_num in range(num_bullies):
            new_bully_pos = (bully_x_ptrs[bully_num][0], bully_y_ptrs[bully_num][0], bully_z_ptrs[bully_num][0])

            #At some point, might want to change this to being close to a specific
            #spot instead of being within a certain annulus about the starting point
            dist = ((new_bully_pos[0] - start_bully_pos[0])**2 +
                    #(new_bully_pos[1] - start_bully_pos[1])**2 +
                    (new_bully_pos[2] - start_bully_pos[2])**2)**.5
            
            if dist > 200 and dist < 1000:
                with open('bullies_results.txt', 'a') as f:
                    tup = (start_bully_pos[0], start_bully_pos[1], start_bully_pos[2],
                           bully_origins[bully_num][0], bully_origins[bully_num][1],
                           iter_frame + 1, new_bully_pos[0], new_bully_pos[1], new_bully_pos[2],
                           dist, dist/bully_origins[bully_num][1], bully_yaw_1_ptrs[bully_num][0],
                           bully_hspd_ptrs[bully_num][0])
                    #print(tup)
                    f.write(str(tup))


# %%
state_iterator.next_state()


# %%



