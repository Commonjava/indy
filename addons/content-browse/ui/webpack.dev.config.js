const path = require('path')
const webpack = require('webpack')

module.exports = {
  entry: './src/main/js/app.js',
  output: {
    path: path.resolve(__dirname, 'build/content-browse'),
    filename: 'app_bundle.js'
  },
  mode: 'development',
  devtool: 'inline-source-map',
  devServer: {
    contentBase: './build/content-browse',
    port: 3000
  },
  module: {
    rules: [
      { test: /\.js$/, use: 'babel-loader', exclude: /node_modules/ },
      { test: /\.jsx?$/, use: 'babel-loader', exclude: /node_modules/ }
    ]
  }
}
