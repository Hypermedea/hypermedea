(define (problem test)
  (:domain itm-factory)
  (:objects
    i
    ws1
    ws2
    locstorage
    loccharging
    locws1
    locws2
    agv
    m0
    m1
    m2)
  (:init
    (item i)
    (model i m0)
    (isat i locstorage)
    (workstation ws1)
    (workstation ws2)
    (off ws1)
    (off ws2)
    (isat ws1 locws1)
    (isat ws2 locws2)
    (consumesmodel ws1 m0)
    (producesmodel ws1 m1)
    (consumesmodel ws2 m1)
    (producesmodel ws2 m2)
    (haspathto loccharging locstorage)
    (haspathto locstorage locws1)
    (haspathto locws1 locws2)
    (haspathto locws2 locstorage)
    (transportationdevice agv)
    (isat agv loccharging))
  (:goal (and
    (model i m2)
    (isat i locstorage))))