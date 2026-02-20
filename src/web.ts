import { WebPlugin } from '@capacitor/core';

import type { NetsEasyPlugin, StartPaymentOptions, PaymentResult } from './definitions';

export class NetsEasyWeb extends WebPlugin implements NetsEasyPlugin {
  async startPayment(_options: StartPaymentOptions): Promise<PaymentResult> {
    throw this.unimplemented(
      'Nets Easy native checkout is not available on web. Use the Nets Easy JavaScript SDK (Dibs.Checkout) for web payments.',
    );
  }
}
