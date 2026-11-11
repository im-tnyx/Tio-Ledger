import SwiftUI
import TioLedgerShared
import UIKit

@main
struct TioLedgerApp: App {
    var body: some Scene {
        WindowGroup {
            IosRootView()
        }
    }
}

private struct IosRootView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        IosAppKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
