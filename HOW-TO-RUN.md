# How to run

```
Usage: bookies <filepath> <tickSize> <depth>

Order book on a financial exchange

Options and flags:
    --help
        Display this help text.
    --version, -v
        Print the version number and exit.
```

Examples:

```
> sbt "run src/main/resources/data1.txt 10.0 2"
50.0,40,60.0,10
40.0,40,70.0,20
```

```
> sbt "run src/main/resources/data2.txt 10.0 3"
50.0,40,80.0,25
40.0,40,70.0,20
0.0,0,60.0,10
```

```
> sbt "run src/main/resources/data3.txt 10.0 3"
50.0,40,60.0,10
40.0,40,0.0,0
0.0,0,0.0,0
```
