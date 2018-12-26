#Type Specializer

Type Specializer makes it easy for specific implementations (subclasses)
to be returned under a declared type (the superclass) depending on the type parameters
the factory method is provided.

#How to Use

In order to get specific implementation `I` under the declared type `A`,
where `I` extends `A`, assuming:

`F` represents the class that holds the factory method.

`FM` represents the factory method.

`T...` represents the type parameters. 

`CT...` represents the `Class` instances associated with the type parameters.

<b>One would do:</b>

`A<T...> specificImplementation = F.FM(CT...);`

#Upcoming Additions

Automatically fetching specialized type based on subclass declarations.</li>

Criteria for types provided other than exact match to required specialized types (`super` and `extends`).

Applying criteria to types based on attributes other than position. (For
example applying criteria to *any* type).
