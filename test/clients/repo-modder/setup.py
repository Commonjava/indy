#!/usr/bin/env python2
#
# Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


from setuptools import setup, find_packages

setup(
    zip_safe=True,
    name='repo_modder',
    version="0.1",
    long_description="This is a test utility for the Indy package repository manager",
    classifiers=[
      "Development Status :: 3 - Alpha",
      "Intended Audience :: Developers",
      "License :: OSI Approved :: Apache Public License",
      "Programming Language :: Python :: 3",
      "Topic :: Software Development :: Build Tools",
      "Topic :: Utilities",
    ],
    keywords='indy maven build java npm javascript',
    author='John Casey',
    author_email='jdcasey@commonjava.org',
    url='https://github.com/Commonjava/indy',
    license='APLv2',
    install_requires=[
      "requests",
      "click",
      "ruamel.yaml"
    ],
    entry_points={
      'console_scripts': [
        'repo-modder = repo_modder:run',
      ],
    }
)

