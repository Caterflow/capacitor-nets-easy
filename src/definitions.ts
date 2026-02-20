export interface NetsEasyPlugin {
  /**
   * Present the Nets Easy checkout and process a payment.
   *
   * On iOS, this presents the MiaCheckoutController (full-screen WebView).
   * On Android, this launches the MiA SDK checkout Activity.
   *
   * The promise resolves when the user completes, cancels, or the payment fails.
   * It never rejects for payment outcomes — only for plugin-level errors
   * (e.g. missing parameters, platform not supported).
   */
  startPayment(options: StartPaymentOptions): Promise<PaymentResult>;
}

export interface StartPaymentOptions {
  /**
   * The payment ID returned by the Nets Easy Create Payment API
   * (`POST /v1/payments` → `paymentId`).
   */
  paymentId: string;

  /**
   * The checkout URL for the hosted payment page.
   * This is the `hostedPaymentPageUrl` from the Create Payment API response
   * when using `integrationType: "HostedPaymentPage"`.
   */
  checkoutUrl: string;

  /**
   * Custom return URL scheme used to detect payment completion.
   * Defaults to `"{bundleId}://netseasy/return"`.
   *
   * Must match the `returnUrl` / `checkout.returnUrl` used when
   * creating the payment on the backend.
   */
  returnUrl?: string;

  /**
   * Custom cancel URL scheme used to detect payment cancellation.
   * Defaults to `"{bundleId}://netseasy/cancel"`.
   *
   * Must match the `cancelUrl` / `checkout.cancelUrl` used when
   * creating the payment on the backend.
   */
  cancelUrl?: string;
}

export type PaymentResultStatus = 'completed' | 'cancelled' | 'failed';

export interface PaymentResult {
  /** The outcome of the payment flow. */
  status: PaymentResultStatus;

  /** The paymentId that was passed in (echoed back for convenience). */
  paymentId: string;

  /** Error message when status is 'failed'. */
  error?: string;
}
