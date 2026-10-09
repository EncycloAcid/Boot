// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "Boot",
    platforms: [.macOS(.v14)],
    products: [
        .library(name: "BootCore", targets: ["BootCore"]),
        .executable(name: "Boot", targets: ["BootApp"]),
        .executable(name: "boot-cli", targets: ["BootCLI"]),
    ],
    targets: [
        // The whole simulation. Foundation only, so it builds and tests on Linux too.
        .target(name: "BootCore"),
        // The macOS SwiftUI game. On other platforms it compiles to a stub.
        .executableTarget(name: "BootApp", dependencies: ["BootCore"]),
        // Terminal harness for playtesting and balance runs.
        .executableTarget(name: "BootCLI", dependencies: ["BootCore"]),
        .testTarget(name: "BootCoreTests", dependencies: ["BootCore"]),
    ]
)
