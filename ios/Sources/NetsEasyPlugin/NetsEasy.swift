import Foundation
import UIKit
import Mia

@objc public class NetsEasy: NSObject {

    @objc public func createCheckoutController(
        paymentId: String,
        paymentURL: String,
        redirectURL: String?,
        cancelURL: String?,
        success: @escaping (MiaCheckoutController) -> Void,
        cancellation: @escaping (MiaCheckoutController) -> Void,
        failure: @escaping (MiaCheckoutController, Error) -> Void
    ) -> MiaCheckoutController {
        return MiaSDK.checkoutControllerForPayment(
            withID: paymentId,
            paymentURL: paymentURL,
            isEasyHostedWithRedirectURL: redirectURL,
            cancelURL: cancelURL,
            success: success,
            cancellation: cancellation,
            failure: failure
        )
    }
}
