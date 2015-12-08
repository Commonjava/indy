Indy is a simple repository manager for [Apache Maven](http://maven.apache.org/) and other build tools using the Maven repository format.

After building, a variety of standalone application launchers are available:

* **Savant** - Includes all add-ons. See: `deployments/launchers/savant/target/indy-launcher-savant-*-launcher.tar.gz`
* **Minimum** - REST server (no UI) with minimal add-ons. See: `deployments/launchers/rest-min/target/indy-launcher-rest-min-*-launcher.tar.gz`
* **EasyProx** - Includes basic ease-of-use add-ons. See: `deployments/launchers/easyprox/target/indy-launcher-easyprox-*-launcher.tar.gz`

To use a launcher, simply unpack it to the directory of your choice (it will create an `indy` subdirectory). Then, run `bin/indy.sh`.

For more information, see [the Indy Docs](http://commonjava.github.io/indy/).
