# tiger_quant_scala

## Background
[tiger_quant](https://github.com/tigerfintech/tiger_quant) is a quantitative development framework created by tiger broker.
It is a Java framework, integrates the tiger SDK, which encapsulates the API interface of this broker.
On top of that, it adds storage support, event handling, order management, algorithm framework, and other functionalities,
making it convenient for developers to develop new quantitative trading algorithms based on it.

## Motivation
This project is a complete rewrite of original `tiger_quant`, developed purely in Scala with a purely asynchronous and purely functional style.
On one hand, the project is aiming to familiarize myself the usage of the `cats` ecosystem through the development work.
On the other hand, I'm hoping that in future I can run my quantitative trading strategies on this platform.

## Status
This project is currently in active development and should not be used in a production environment yet.
