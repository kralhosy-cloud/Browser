// Web content must not access native bridge / helper
test('page cannot fetch pipe', async () => {
  const page = await browser.newPage();
  const err = await page.evaluate(() => {
    try { fetch('pipe://calyra-helper'); return 'no-block'; } catch(e){ return 'blocked'; }
  });
  expect(err).toBe('blocked');
});
