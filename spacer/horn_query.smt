(set-logic HORN)
(declare-fun Start (Int Int Int Int ) Bool)
(assert (Start 1 0 0 1))
(assert (Start 0 1 0 3))
(assert (Start 0 0 0 0))
(assert (Start 1 1 1 1))
(assert (forall ((x_0_0 Int) (x_0_1 Int) (x_0_2 Int) (x_0_3 Int) (x_1_0 Int) (x_1_1 Int) (x_1_2 Int) (x_1_3 Int) )
 (=> (and (Start x_0_0 x_0_1 x_0_2 x_0_3) (Start x_1_0 x_1_1 x_1_2 x_1_3) ) (Start (+ x_0_0 x_1_0) (+ x_0_1 x_1_1) (+ x_0_2 x_1_2) (+ x_0_3 x_1_3)))))


(assert (forall ( (x_0 Int)  (x_1 Int)  (x_2 Int)  (x_3 Int) )
	(=> (Start  x_0  x_1  x_2  x_3 ) (not (and
		(= x_0 1)
		(= x_1 1)
		(= x_2 0)
		(= x_3 4)
)))))
(check-sat)