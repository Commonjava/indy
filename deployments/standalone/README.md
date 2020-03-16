====
    Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
====

* This module is used to generate a standalone launcher for indy, which can run indy in standalone mode which does not enable path mapped storage with cassandra support for some purposes like local testing or debugging.  

* To use this launcher, please make sure the main default configuration has enabled the "standalone" parameter like: standalone=true

* To build this launcher, please use -Pstandalone profile in maven build.