// bulma-css-vars.config.js
// const { hsl, rgb } = require('bulma-css-vars');

// color names have to match bulma's $variable-name, without '$'
// values will be used for initial colors and fallback
const appColors = {
  info: '#a9ffc1',
  primary: '#009AD7',
};

module.exports = {
  cssFallbackOutputFile:
    'src/assets/stylesheets/bulma-generated/generated-fallback.css',
  jsOutputFile: 'src/assets/stylesheets/bulma-generated/bulma-colors.ts',
  sassEntryFile: 'src/assets/stylesheets/main.sass',
  sassOutputFile:
    'src/assets/stylesheets/bulma-generated/generated-bulma-vars.sass',
  colorDefs: appColors,
  transition: '0.5s ease',
};
