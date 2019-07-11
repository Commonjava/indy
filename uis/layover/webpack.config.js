const path = require('path');
const CopyWebpackPlugin = require('copy-webpack-plugin');

const Assets = require('./assets');

module.exports = 
{
    "mode": "production",
    "entry": "./app/index.js",
    "output": {
        "path": path.resolve(__dirname, 'dist'),
        "filename": "indy.bundle.js"
    },
    plugins: [
      new CopyWebpackPlugin(
        Assets.map(asset => {
          return {
            from: path.resolve(__dirname, `./${asset.from?asset.from:asset}`),
            to: path.resolve(__dirname, `./dist/${asset.to?asset.to:asset}`)
          };
        })
      )
    ],
    devServer: {
      port: 3000,
      contentBase: './dist'
    }
}
