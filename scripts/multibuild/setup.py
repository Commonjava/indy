#!/usr/bin/env python2

from setuptools import setup, find_packages
import sys

# handle python 3
if sys.version_info >= (3,):
    use_2to3 = True
else:
    use_2to3 = False

setup(
    zip_safe=True,
    use_2to3=use_2to3,
    name='mb',
    version='0.0.1',
    long_description='Orchestrate build performance tests against Indy + NFS setup with various configuration options',
    classifiers=[
      "Development Status :: 3 - Alpha",
      "Intended Audience :: Developers",
      "License :: OSI Approved :: GNU General Public License (GPL)",
      "Programming Language :: Python :: 2",
      "Programming Language :: Python :: 3",
      "Topic :: Software Development :: Build Tools",
      "Topic :: Utilities",
    ],
    keywords='indy maven build java ',
    author='John Casey',
    author_email='jdcasey@commonjava.org',
    url='https://github.com/Commonjava/indy-vagrant-nfs',
    license='GPLv3+',
    packages=find_packages(exclude=['ez_setup', 'examples', 'tests']),
    install_requires=[
      "requests",
      "PyYAML",
      "click",
    ],
    entry_points={
      'console_scripts': [
        'multibuild = mb:build',
        'multicheck = mb:check',
      ],
    }
)

