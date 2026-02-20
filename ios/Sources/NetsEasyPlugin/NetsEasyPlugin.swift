import Foundation
import Capacitor
import Mia

@objc(NetsEasyPlugin)
public class NetsEasyPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "NetsEasyPlugin"
    public let jsName = "NetsEasy"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "startPayment", returnType: CAPPluginReturnPromise)
    ]

    private let implementation = NetsEasy()

    @objc func startPayment(_ call: CAPPluginCall) {
        guard let paymentId = call.getString("paymentId") else {
            call.reject("Missing required parameter: paymentId")
            return
        }
        guard let checkoutUrl = call.getString("checkoutUrl") else {
            call.reject("Missing required parameter: checkoutUrl")
            return
        }

        let bundleId = Bundle.main.bundleIdentifier ?? "app"
        let returnUrl = call.getString("returnUrl") ?? "\(bundleId)://netseasy/return"
        let cancelUrl = call.getString("cancelUrl") ?? "\(bundleId)://netseasy/cancel"

        call.keepAlive = true

        let checkoutController = implementation.createCheckoutController(
            paymentId: paymentId,
            paymentURL: checkoutUrl,
            redirectURL: returnUrl,
            cancelURL: cancelUrl,
            success: { [weak self] controller in
                DispatchQueue.main.async {
                    controller.dismiss(animated: true) {
                        call.resolve([
                            "status": "completed",
                            "paymentId": paymentId
                        ])
                        self?.bridge?.releaseCall(call)
                    }
                }
            },
            cancellation: { [weak self] controller in
                DispatchQueue.main.async {
                    controller.dismiss(animated: true) {
                        call.resolve([
                            "status": "cancelled",
                            "paymentId": paymentId
                        ])
                        self?.bridge?.releaseCall(call)
                    }
                }
            },
            failure: { [weak self] controller, error in
                DispatchQueue.main.async {
                    controller.dismiss(animated: true) {
                        call.resolve([
                            "status": "failed",
                            "paymentId": paymentId,
                            "error": error.localizedDescription
                        ])
                        self?.bridge?.releaseCall(call)
                    }
                }
            }
        )

        DispatchQueue.main.async { [weak self] in
            self?.bridge?.viewController?.present(checkoutController, animated: true)
        }
    }
}
