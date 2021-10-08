(define (domain itm-factory)
  (:requirements :adl)
  (:predicates
    (item ?item)
    (busy ?device) ; redundant with (carry) but (exists) not well supported
    (carry ?device ?item)
    (transportationdevice ?device)
    (haspathto ?loc1 ?loc2)
    (isat ?deviceOrWs ?loc)
    (workstation ?ws)
    (on ?ws)
    (off ?ws)
    (consumesmodel ?ws ?model)
    (producesmodel ?ws ?model)
    (model ?item ?model))
  (:action moveto
    :parameters (?device ?from ?to)
    :precondition (and
      (transportationdevice ?device)
      (isat ?device ?from)
      (haspathto ?from ?to))
    :effect (and
      (not (isat ?device ?from))
      (isat ?device ?to)))
  (:action pick
    :parameters (?device ?item ?location)
    :precondition (and
      (transportationdevice ?device)
      (not (busy ?device))
      (isat ?device ?location)
      (item ?item)
      (isat ?item ?location))
    :effect (and
      (busy ?device)
      (carry ?device ?item)
      (not (isat ?item ?location))))
  (:action place
    :parameters (?device ?item1 ?item2 ?location ?ws ?m1 ?m2 ?m3)
    :precondition (and
      (transportationdevice ?device)
      (carry ?device ?item1)
      (isat ?device ?location))
    :effect (and
      (isat ?item1 ?location)
      (not (busy ?device))
      (not (carry ?device ?item1))
      (when
        (and
          (workstation ?ws)
          (isat ?ws ?location)
          (on ?ws)
          (consumesmodel ?ws ?m1)
          (model ?item1 ?m1)
          (producesmodel ?ws ?m3)
          ; (or
          ;   ; 1 item to process
          ;   (not
          ;     (and
          ;       (consumesmodel ?ws ?m2)
          ;       (not (= ?m1 ?m2))))
          ;   ; 2 items to process
          ;   (and
          ;     (isat ?item2 ?location)
          ;     (model ?item2 ?m2)
          ;     (not (= ?m1 ?m2))
          ;     (consumesmodel ?ws ?m2))))
        )
        (and
          (not (model ?item1 ?m1))
          (model ?item1 ?m3)))))
  (:action turnon
    :parameters (?workstation)
    :precondition (workstation ?workstation)
    :effect (when
      (off ?workstation)
      (on ?workstation)))
)