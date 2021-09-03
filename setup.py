#!/usr/bin/env python

from setuptools import setup

setup(
    name='jtabwb',
    version='0.1',
    description='Python bindings for the Java Tableaux Workbench.',
    packages=[
        'jtabwb',
    ],
    include_package_data=True,
    package_data={'jtabwb': [
        'jtabwb-1.0-jar-with-dependencies.jar',
    ]},
    install_requires=[
        'jpype1',
    ],
    python_requires='>=3.6,',
    test_suite='tests',
    zip_safe=False,
    # entry_points={'console_scripts': [
    #     'jtabwb = jtabwb.tool:main',
    # ]}
)
