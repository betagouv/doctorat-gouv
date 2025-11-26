
export default {
  bootstrap: () => import('./main.server.mjs').then(m => m.default),
  inlineCriticalCss: true,
  baseHref: '/',
  locale: undefined,
  routes: [
  {
    "renderMode": 2,
    "route": "/"
  }
],
  entryPointToBrowserMapping: undefined,
  assets: {
    'index.csr.html': {size: 504, hash: '1b34537d8ea919be82f1419dabd8fdd95a0572fc0cc20fae9b30f7cbcc1756fb', text: () => import('./assets-chunks/index_csr_html.mjs').then(m => m.default)},
    'index.server.html': {size: 1017, hash: '3bf32ac9ed2a2fb0a73afb2f1b3b2b971ce0ac1e3bd078d01eca28ba7c4a2927', text: () => import('./assets-chunks/index_server_html.mjs').then(m => m.default)},
    'index.html': {size: 21650, hash: '14744cb801975d3baab57963c69de0ad9cb69f203310a8f6fa12606f3ccecc74', text: () => import('./assets-chunks/index_html.mjs').then(m => m.default)},
    'styles-5INURTSO.css': {size: 0, hash: 'menYUTfbRu8', text: () => import('./assets-chunks/styles-5INURTSO_css.mjs').then(m => m.default)}
  },
};
