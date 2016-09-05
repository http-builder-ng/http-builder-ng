
* configure and http verb methods would need additional methods that take a `java.util.function.Consumer` with the `HttpConfig` argument.
* Add alternate (`java.util.function.*`) versions of methods accepting `Closure`s now.
* Add function variants for parsers and encoders (`Function`/`BiFunction` arguments)
* Probably delegate one version to the other (maybe wrap `Closure` in `Function` variants)

* Documentation 
* Testing

* configure
* verbs
* encoders
* parsers
* interceptors

* opportunity to refactor/reduce after implementation
* consider creating separate interfaces for Groovy vs Java config
* consider pushing the function/closure adapters down into the API so that users dont need to explicitly use them