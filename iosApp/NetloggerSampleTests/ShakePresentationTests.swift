import XCTest
import UIKit
@testable import NetloggerSample

/// Host-app UI integration: dispatches UIKit motion events through the actual sample window.
@MainActor
final class ShakePresentationTests: XCTestCase {
    private var window: UIWindow {
        UIApplication.shared.connectedScenes.compactMap { ($0.delegate as? SceneDelegate)?.window }.first!
    }

    private func eventually(_ condition: () -> Bool) async throws {
        let deadline = Date().addingTimeInterval(10)
        while !condition() && Date() < deadline {
            try await Task.sleep(nanoseconds: 20_000_000)
        }
        XCTAssertTrue(condition(), "UIKit did not reach the expected presentation state")
    }

    override func setUp() async throws {
        try await super.setUp()
        window.makeKeyAndVisible()
        window.rootViewController?.dismiss(animated: false)
        // Let host composition initialize Netlogger, and allow the presentation debounce to expire.
        try await Task.sleep(nanoseconds: 700_000_000)
    }

    override func tearDown() async throws {
        window.rootViewController?.dismiss(animated: false)
        window.makeKeyAndVisible()
        try await super.tearDown()
    }

    func testShakeOpensViewerWithoutStackingAndCanReopenAfterDismissal() async throws {
        let root = try XCTUnwrap(window.rootViewController)
        window.motionEnded(.motionShake, with: nil)
        try await eventually { root.presentedViewController != nil && !root.presentedViewController!.isBeingPresented }
        let viewer = try XCTUnwrap(root.presentedViewController)
        XCTAssertTrue(viewer is UINavigationController)
        XCTAssertTrue(viewer.view.window === window)

        window.motionEnded(.motionShake, with: nil)
        XCTAssertTrue(root.presentedViewController === viewer)
        XCTAssertNil(viewer.presentedViewController)

        viewer.dismiss(animated: false)
        try await eventually { root.presentedViewController == nil }
        try await Task.sleep(nanoseconds: 600_000_000)
        window.motionEnded(.motionShake, with: nil)
        try await eventually { root.presentedViewController != nil }
    }

    func testNonShakeMotionDoesNotOpenViewer() {
        window.motionEnded(.none, with: nil)
        XCTAssertNil(window.rootViewController?.presentedViewController)
    }

    func testHiddenWindowDoesNotPresentViewer() {
        window.isHidden = true
        window.motionEnded(.motionShake, with: nil)
        XCTAssertNil(window.rootViewController?.presentedViewController)
    }
}
