plugins {
    id("org.jetbrains.kotlin.konan") version "0.8.2"
}

//konan {
//    // Compile this component for 64-bit MacOS, Linux and Windows.
//    targets = mutableListOf("macos_x64", "linux_x64", "mingw_x64")
//}

konanArtifacts {
    program("SpiralBridgeNativeMacOSRun")
}
