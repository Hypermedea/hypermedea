(define (problem pb)
  (:domain itm-factory)
  (:objects
    agv
    {objects})
  (:init
    (transportationdevice agv)
    (isat agv loccharging)
    {init})
  (:goal (or
    {goal})))