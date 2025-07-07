// swift-tools-version: 6.1
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "swift-calculator",
    targets: [
        .executableTarget(
            name: "swift-calculator",
            swiftSettings: [
                .enableExperimentalFeature("Extern")
            ]
        )
    ]
)
