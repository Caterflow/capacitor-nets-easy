# capacitor-nets-easy

Capacitor 6.x plugin for [Nets Easy](https://developer.nexigroup.com/nets-easy/en-EU/) (Nexi Group) in-app payments on iOS and Android.

Wraps the official Nets Easy mobile SDKs ([iOS](https://github.com/Nets-eCom/Nets-Easy-iOS-SDK) / [Android](https://github.com/Nets-eCom/Nets-Easy-Android-SDK)) so your Capacitor app can accept payments natively — no browser redirects.

## Features

- In-app payment checkout (WebView-based, stays inside the app)
- Card payments (Visa, MasterCard, Dankort, AmEx, etc.)
- MobilePay (with native app-switching on iOS/Android)
- Apple Pay (iOS)
- Google Pay (Android)
- "Remember me" / card tokenization
- Subscriptions and unscheduled charges
- Test and production environments

> **Note:** Payment methods are configured server-side when you create the payment via the [Nets Easy API](https://developer.nexigroup.com/nets-easy/en-EU/api/payment-v1/). The plugin presents whatever payment options your backend has enabled.

## Prerequisites

- Capacitor 6.x
- iOS 15+ / Android API 24+
- A [Nets Easy merchant account](https://portal.dibspayment.eu/) with API keys
- A backend that creates payments via the [Nets Easy Payment API](https://developer.nexigroup.com/nets-easy/en-EU/api/payment-v1/)

## Install

```bash
npm install capacitor-nets-easy
npx cap sync
```

### iOS Setup

The Nets Easy iOS SDK (`Mia.xcframework`) is not available via CocoaPods or SPM, so it must be downloaded separately:

```bash
# From the plugin directory (or your project root):
npx capacitor-nets-easy download-ios-sdk

# Or manually:
bash node_modules/capacitor-nets-easy/scripts/download-ios-sdk.sh
```

This downloads `Mia.xcframework` from the [official GitHub repository](https://github.com/Nets-eCom/Nets-Easy-iOS-SDK) into the plugin's `ios/Frameworks/` directory.

Then sync:

```bash
npx cap sync ios
```

### Android Setup

The Android SDK is pulled automatically from Maven Central. No additional setup required.

If you use ProGuard/R8, the plugin includes rules to keep the SDK classes.

## How It Works

The payment flow has two parts:

1. **Your backend** creates a payment via `POST /v1/payments` with `integrationType: "HostedPaymentPage"` and receives a `paymentId` + `hostedPaymentPageUrl`
2. **Your app** calls `NetsEasy.startPayment()` with those values — the plugin presents the checkout UI and returns the result

```
┌─────────────┐     POST /v1/payments     ┌──────────────┐
│  Your App   │ ───────────────────────── │ Your Backend │
│             │ ◄─ paymentId + checkout ── │              │
│             │         URL               │              │
│             │                           └──────────────┘
│             │                                  │
│  NetsEasy.  │     Hosted Payment Page          │
│  start      │ ──── presents checkout ────      │
│  Payment()  │     in native WebView            │
│             │                                  │
│  ◄── result ┤     (completed/cancelled/        │
│             │      failed)                     │
└─────────────┘                                  │
                    POST /charges (backend) ─────┘
```

## Usage

### Basic Example

```typescript
import { NetsEasy } from 'capacitor-nets-easy';

// 1. Create payment on your backend
const response = await fetch('/api/create-payment', {
  method: 'POST',
  body: JSON.stringify({ amount: 10000, currency: 'DKK' }),
});
const { paymentId, checkoutUrl } = await response.json();

// 2. Present the checkout
const result = await NetsEasy.startPayment({
  paymentId,
  checkoutUrl,
});

// 3. Handle the result
switch (result.status) {
  case 'completed':
    console.log('Payment completed:', result.paymentId);
    // Verify and charge on your backend
    await fetch(`/api/charge-payment/${result.paymentId}`, { method: 'POST' });
    break;
  case 'cancelled':
    console.log('User cancelled the payment');
    break;
  case 'failed':
    console.error('Payment failed:', result.error);
    break;
}
```

### React / Capacitor Example

```tsx
import { NetsEasy } from 'capacitor-nets-easy';
import { Capacitor } from '@capacitor/core';

function PaymentButton({ orderId }: { orderId: string }) {
  const [loading, setLoading] = useState(false);

  const handlePay = async () => {
    setLoading(true);
    try {
      // Create payment on backend (use HostedPaymentPage for native)
      const res = await api.createPayment(orderId, {
        platform: Capacitor.isNativePlatform() ? 'native' : 'web',
      });

      if (Capacitor.isNativePlatform()) {
        // Native: use the plugin
        const result = await NetsEasy.startPayment({
          paymentId: res.paymentId,
          checkoutUrl: res.hostedPaymentUrl,
        });

        if (result.status === 'completed') {
          // Payment succeeded — refresh order status
        } else if (result.status === 'cancelled') {
          // User cancelled
        } else {
          // Payment failed
          alert(result.error);
        }
      } else {
        // Web: use the Nets Easy JavaScript SDK (Dibs.Checkout)
        // See: https://developer.nexigroup.com/nets-easy/en-EU/docs/web-integration/
      }
    } finally {
      setLoading(false);
    }
  };

  return <button onClick={handlePay} disabled={loading}>Pay</button>;
}
```

## Backend Setup

When creating payments for native apps, use `integrationType: "HostedPaymentPage"`:

```json
POST https://test.api.dibspayment.eu/v1/payments
Authorization: <your-secret-key>
Content-Type: application/json
commercePlatformTag: iOSSDK  // or AndroidSDK

{
  "order": {
    "items": [
      {
        "reference": "item-001",
        "name": "Lunch Ticket",
        "quantity": 1,
        "unit": "pcs",
        "unitPrice": 10000,
        "taxRate": 0,
        "taxAmount": 0,
        "grossTotalAmount": 10000,
        "netTotalAmount": 10000
      }
    ],
    "amount": 10000,
    "currency": "DKK",
    "reference": "order-123"
  },
  "checkout": {
    "integrationType": "HostedPaymentPage",
    "returnUrl": "com.example.myapp://netseasy/return",
    "cancelUrl": "com.example.myapp://netseasy/cancel",
    "termsUrl": "https://example.com/terms"
  }
}
```

The response includes:

```json
{
  "paymentId": "0260...",
  "hostedPaymentPageUrl": "https://checkout.dibspayment.eu/v1/checkout.js?v=1&paymentId=0260..."
}
```

Pass `paymentId` and `hostedPaymentPageUrl` as `checkoutUrl` to `startPayment()`.

### Return URLs

The plugin auto-generates return/cancel URLs from your app's bundle identifier:

- **iOS**: `{Bundle.main.bundleIdentifier}://netseasy/return`
- **Android**: `{applicationId}://netseasy/cancel`

These must match the `returnUrl` and `cancelUrl` in your Create Payment API request. You can override them via the `returnUrl`/`cancelUrl` options if needed.

> These URLs are intercepted by the SDK's internal WebView — you do **not** need to register custom URL schemes in your app.

### Environments

| Environment | API Base URL | Checkout JS URL |
|---|---|---|
| Test | `https://test.api.dibspayment.eu` | `https://test.checkout.dibspayment.eu/v1/checkout.js?v=1` |
| Production | `https://api.dibspayment.eu` | `https://checkout.dibspayment.eu/v1/checkout.js?v=1` |

The plugin has no environment toggle — it's determined by which API URLs your backend uses.

### Payment Methods

Payment methods are configured server-side — the plugin itself requires no code changes per method. All enabled methods appear automatically on the hosted checkout page.

If you omit `paymentMethodsConfiguration` from your Create Payment request, all methods enabled on your merchant account are shown. To show only specific methods, list them explicitly:

```json
{
  "paymentMethodsConfiguration": [
    { "name": "Card" },
    { "name": "GooglePay" }
  ]
}
```

#### Cards

Visa, MasterCard, Dankort, AmEx — enabled by default. No additional setup.

#### MobilePay

The SDK handles app-switching automatically (iOS v1.4.0+ / Android). Enable MobilePay on your merchant account in the [Easy Portal](https://portal.dibspayment.eu/). On iOS, add `mobilepay` to `LSApplicationQueriesSchemes` in your `Info.plist` for app-switching to work.

#### Google Pay (Android)

Google Pay is rendered as a web-based button on the hosted checkout page inside the WebView. No Android app changes, manifest entries, or Google Pay SDK dependencies are needed on the client side.

**Setup:**

1. **Contact Nexi** at `ecom-salessupport@nets.eu` to enable Google Pay on your merchant ID (test and production are activated separately)
2. That's it — once activated, the Google Pay button appears on the checkout page automatically

To test: you need a real Google account. In the test environment, a Google test card is provided automatically — no real card needed.

> **Tip:** Avoid showing Google Pay as the *only* payment method. If the user's device doesn't support it, they'll see no payment options. Always include `Card` alongside `GooglePay` in `paymentMethodsConfiguration`.

See the [Nets Easy Google Pay docs](https://developer.nexigroup.com/nets-easy/en-EU/docs/google-pay/) for details.

#### Apple Pay (iOS)

Like Google Pay, Apple Pay is handled by the hosted checkout page. No native Apple Pay SDK integration is needed in the plugin.

**Setup:**

1. **Contact Nexi** at `ecom-salessupport@nets.eu` to enable Apple Pay on your merchant ID
2. Follow the [Apple Pay setup guide](https://developer.nexigroup.com/nets-easy/en-EU/docs/apple-pay/) to configure your Apple Pay certificates with Nexi

Apple Pay requires iOS 16+.

#### Remember Me / Tokenization

Use [unscheduled subscriptions](https://developer.nexigroup.com/nets-easy/en-EU/docs/unscheduled-subscriptions-ucof/) for storing cards and charging later. Configured in the Create Payment API request — no plugin changes needed.

### Test Cards

| Card Number | Type | 3DS |
|---|---|---|
| 4111 1111 1111 1111 | Visa | No |
| 5413 0000 0000 0000 | MasterCard | No |
| 4917 6100 0000 0000 | Visa | Yes |

Expiry: any future date. CVV: any 3 digits. See [Nets Easy test environment docs](https://developer.nexigroup.com/nets-easy/en-EU/docs/test-environment/).

## API Reference

### `startPayment(options)`

Present the Nets Easy checkout and process a payment.

| Param | Type | Description |
|---|---|---|
| `options.paymentId` | `string` | **Required.** Payment ID from the Create Payment API response. |
| `options.checkoutUrl` | `string` | **Required.** The `hostedPaymentPageUrl` from the Create Payment API response. |
| `options.returnUrl` | `string` | Custom return URL. Defaults to `{bundleId}://netseasy/return`. |
| `options.cancelUrl` | `string` | Custom cancel URL. Defaults to `{bundleId}://netseasy/cancel`. |

**Returns:** `Promise<PaymentResult>`

| Property | Type | Description |
|---|---|---|
| `status` | `'completed' \| 'cancelled' \| 'failed'` | The outcome of the payment flow. |
| `paymentId` | `string` | The payment ID (echoed back). |
| `error` | `string \| undefined` | Error message when `status` is `'failed'`. |

> The promise resolves for all payment outcomes (completed, cancelled, failed). It only rejects for plugin-level errors like missing parameters or running on an unsupported platform.

## SDK Versions

This plugin bundles / depends on:

| Platform | SDK | Version |
|---|---|---|
| iOS | [Nets-Easy-iOS-SDK](https://github.com/Nets-eCom/Nets-Easy-iOS-SDK) (Mia.xcframework) | 1.6.1 |
| Android | [Nets-Easy-Android-SDK](https://github.com/Nets-eCom/Nets-Easy-Android-SDK) (eu.nets.mia:mia-sdk) | 1.6.1 |

## Troubleshooting

### iOS: `Mia.xcframework not found`

Run the download script:

```bash
bash node_modules/capacitor-nets-easy/scripts/download-ios-sdk.sh
npx cap sync ios
```

### Android: Payment result not received

Ensure the plugin is registered in your app's `MainActivity.java`:

```java
import com.caterflow.capacitor.netseasy.NetsEasyPlugin;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(NetsEasyPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
```

### Payment stuck / never returns

Make sure the `returnUrl` and `cancelUrl` in your Create Payment API request match what the plugin generates (or what you pass as options). The SDK detects payment completion by intercepting navigation to these URLs.

### MobilePay app-switching not working

- iOS: Ensure `LSApplicationQueriesSchemes` includes `mobilepay` in your `Info.plist`
- Android: MobilePay should work out of the box if enabled server-side

## License

MIT
