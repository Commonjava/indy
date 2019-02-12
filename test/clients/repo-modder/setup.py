#!/usr/bin/env python2

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

