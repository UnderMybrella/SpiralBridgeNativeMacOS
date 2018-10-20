import platform.posix.getpid
import kotlin.math.roundToLong

fun main(args: Array<String>) {
    if (args.size < 2)
        error("Need operation flag and pid")

    val operation = args[0]
    val native = SpiralNativeBridgeMacOS(args[1].toInt())

    when (operation) {
        "-B" -> println(native.benchmark(if (args.size == 2) 10 else args[2].toInt()).joinToString())
    }
}