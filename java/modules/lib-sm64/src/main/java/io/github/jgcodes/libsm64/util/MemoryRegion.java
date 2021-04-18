package io.github.jgcodes.libsm64.util;

/**
 * A region of virtual address space that can be stored and restored to essentially "rewind".
 */
public record MemoryRegion(int address, int size) {}
