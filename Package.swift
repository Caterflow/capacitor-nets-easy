// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorNetsEasy",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "CapacitorNetsEasy",
            targets: ["NetsEasyPlugin"]
        )
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "6.0.0")
    ],
    targets: [
        .binaryTarget(
            name: "Mia",
            path: "ios/Frameworks/Mia.xcframework"
        ),
        .target(
            name: "NetsEasyPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm"),
                "Mia"
            ],
            path: "ios/Sources/NetsEasyPlugin"
        )
    ]
)
