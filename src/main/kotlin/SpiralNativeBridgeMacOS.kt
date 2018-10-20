import kotlinx.cinterop.*
import kotlinx.cinterop.ByteVar
import platform.darwin.*
import platform.osx.proc_pidpath
import platform.osx.proc_regionfilename
import platform.posix.MAXPATHLEN
import kotlin.system.measureNanoTime

class SpiralNativeBridgeMacOS(val pid: Int) {
    companion object {
        val VALUE_MASK = 0x0000FFFF.toInt()
        val COUNT_MASK = 0xFFFF0000.toInt()
    }

    val task: Int
    val ourTask: Int = mach_task_self_
    val detail: String

    val regions: Array<MemoryRegion>
    val remaps: Array<CPointer<ByteVar>>

    //    val remappedRegions: MutableMap<Long, CPointer<integer_tVar>> = HashMap()
    fun benchmark(count: Int = 10): List<Long> {
        val candidates: Array<IntArray> = regions.map { region -> IntArray(region.size.toInt()) }.toTypedArray()

        var currentCount: Int
        var currentValue: Int
        var currentMemValue: Int
        var ptr: CPointer<ByteVar>

        try {
            return (0 until count).map {
                regions.mapIndexed { index, region ->
                    measureNanoTime {
                        ptr = remaps[index]

                        for (i in 0 until (region.size - (region.size % 2) - 1).toInt()) {
                            currentCount = (candidates[index][i] and COUNT_MASK) shr 16
                            currentValue = (candidates[index][i] and VALUE_MASK) shr 0
                            currentMemValue = ((ptr[i].toInt() and 0xFF shl 8) or (ptr[i + 1].toInt() and 0xFF))

                            if (currentValue == 1 && currentMemValue == 2) {
                            } else if (currentValue == 2 && currentMemValue == 4) {
                            } else if (currentValue == 4 && currentMemValue == 8) {
                            } else if (currentValue == 8 && currentMemValue == 1) {
                            } else if (currentValue == 0) {
                            } else if (currentValue != currentMemValue) {
                            }

                            candidates[index][i] = ((0 and VALUE_MASK) shl 16) or (0 and VALUE_MASK)
                        }
                    }
                }.sum()
            }
        } finally {
        }
    }

    //
//    fun synchronise(regions: Array<Pair<Long, Long>>, a: Int, b: Int, c: Int, d: Int) {
//
//    }
//

    init {
        val detailBuffer = nativeHeap.allocArray<ByteVar>(MAXPATHLEN)
        try {
            proc_pidpath(pid, detailBuffer, MAXPATHLEN)

            detail = detailBuffer.toKString()
        } finally {
            nativeHeap.free(detailBuffer)
        }

        val taskVar: IntVar = nativeHeap.alloc()
        try {
            val kret = task_for_pid(ourTask, pid, taskVar.ptr)
            task = taskVar.value
        } finally {
            nativeHeap.free(taskVar)
        }

        val regions: MutableList<MemoryRegion> = ArrayList()

        val address: LongVar = nativeHeap.alloc()
        address.value = 0

        val size: LongVar = nativeHeap.alloc()
        val info: vm_region_basic_info_64_tVar = nativeHeap.alloc()
        val infoCount: IntVar = nativeHeap.alloc()
        val objectName: IntVar = nativeHeap.alloc()
        val depth: IntVar = nativeHeap.alloc()

        try {
            infoCount.value = sizeOf<vm_region_basic_info_64>().toInt() / 4
            var kret: Int? = 0
            objectName.value = 0
            depth.value = 1

            while (kret == 0) {
                kret = vm_region_64(task, address.ptr, size.ptr, VM_REGION_BASIC_INFO_64, info.ptr.reinterpret(), infoCount.ptr, objectName.ptr) and 0x000000FF

                if (kret != 0)
                    break

                val regionInfo = info.ptr.reinterpret<vm_region_basic_info_64>()[0]

                regions.add(MemoryRegion(address.value, size.value, regionInfo.protection, run {
                    val buffer = nativeHeap.allocArray<ByteVar>(MAXPATHLEN)
                    try {
                        proc_regionfilename(pid, address.value, buffer, MAXPATHLEN)

                        return@run buffer.toKString()
                    } finally {
                        nativeHeap.free(buffer)
                    }
                }))

                address.value += size.value
            }

            this.regions = regions
                    .filter { region -> region.canWrite && (region.detail.isBlank() || region.detail == detail) } //We only want regions that are ours, or private (blank)
                    .take(2) //We only want two
                    .toTypedArray()
        } finally {
            nativeHeap.free(address)
            nativeHeap.free(size)
            nativeHeap.free(info)
            nativeHeap.free(infoCount)
            nativeHeap.free(objectName)
            nativeHeap.free(depth)
        }

        this.remaps = regions.map { region ->
            val newAddress: LongVar = nativeHeap.alloc()
            val curProtection: IntVar = nativeHeap.alloc()
            val maxProtection: IntVar = nativeHeap.alloc()

            val kret = vm_remap(ourTask, newAddress.ptr, region.size, 0, 1, task, region.start, 0, curProtection.ptr, maxProtection.ptr, VM_INHERIT_SHARE) and 0x000000FF

            if (kret != 0)
                error("Remapping $region yields $kret")

            return@map newAddress.value.toCPointer<ByteVar>() ?: error("Cannot create pointer for ${newAddress.value}")
        }.toTypedArray()
    }
}

