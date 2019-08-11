# Type Specializer

Type Specializer makes it easy for specific implementations (subclasses)
to be returned under a declared type (the superclass) depending on the type parameters
the factory method is provided.

## How to Use

In order to get specific implementation `I` under the declared type `A`,
where `I` extends `A`, assuming:

`F` represents the class that holds the factory method.

`FM` represents the factory method.

`T...` represents the type parameters. 

`CT...` represents the `Class` instances associated with the type parameters.

<b>One would do:</b>

`A<T...> specificImplementation = F.FM(CT...);`

## Upcoming Additions

1. Automatically fetching specialized type based on subclass declarations rather than explicitly needing to provide them through the annotation.

2. Criteria for types provided that extends past an exact match (`super` and `extends`). For example, a type argument of a subclass of a specialized argument would result in a specialized instance.

3. Applying criteria to types based on attributes other than position. (For example, applying criteria to *any* type that is present among the type arguments list rather than the type argument at a certain index).
