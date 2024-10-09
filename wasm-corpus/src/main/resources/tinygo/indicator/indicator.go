package main

import (
	"fmt"
	"github.com/cinar/indicator"
)

func main() {
	res := indicator.VolumeWeightedAveragePrice(
	    5, []float64{1,2,3,4,5}, []float64{1,2,3,4,5})
	fmt.Printf("%v\n", res)
}
