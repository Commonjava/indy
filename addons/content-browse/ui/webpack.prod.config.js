const path = require('path')
const webpack = require('webpack')

module.exports = {
  entry: './src/main/js/app.js',
  output: {
    path: path.resolve(__dirname, 'build/content-browse'),
    filename: 'app_bundle.js'
  },
  mode: 'production',
  plugins: [
    new webpack.DefinePlugin({
      'process.env': {
        'NODE_ENV': JSON.stringify('production')
      }
    })
  ],
  module: {
    rules: [
      { test: /\.js$/, use: 'babel-loader', exclude: /node_modules/ },
      { test: /\.jsx?$/, use: 'babel-loader', exclude: /node_modules/ }
    ]
  }
}
