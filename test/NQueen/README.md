# Multiple Implementation of NQueens

These are the different implementation of NQueens test which is used for check the new library can work fine.

> We use NQueens to verify our new (BDD/NDD/BDD Vector) library.

## BDD (use jdd library)

An example copied from `jdd/examples/BDDQueens.java`.

## NDD (a simple implementation)

A simple baseline version of NDD without garbage collection, operation cache and other optimization.

NDD separates fields cuz the to-be-encoded fields in network are independent of each other. We can encode each field by NDD node and connect with its children by multiple edges, where we use BDD (jdd library in default NDD version).

## BDD Vector (compared to NDD)

A simple baseline version of BDD Vector, which needs to be compared to NDD's running speed and memory usage.

BDD Vector also combines the thinking of separating fields, but uses an array (vector) to present the structure of BDD fields. BDD Vector uses different operate methods or modes. For example, we implement three `NotRec`:
1. `NotRec`
2. `NotRecDirectly`: disassemble the vector and optimize fetching BDD.
3. `NotRecBackBDD` (recommended): convert BDD Vector to a whole BDD with field replacement and then convert back to BDD Vector.