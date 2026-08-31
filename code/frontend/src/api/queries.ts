import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  cancelBooking,
  createBooking,
  createEventType,
  getEventType,
  getOwner,
  listBookings,
  listEventTypes,
  listSlots,
} from './endpoints'

export const queryKeys = {
  owner: ['owner'] as const,
  eventTypes: ['event-types'] as const,
  eventType: (id: string) => ['event-types', id] as const,
  slots: (id: string) => ['event-types', id, 'slots'] as const,
  bookings: ['bookings'] as const,
}

export function useOwner() {
  return useQuery({ queryKey: queryKeys.owner, queryFn: getOwner })
}

export function useEventTypes() {
  return useQuery({ queryKey: queryKeys.eventTypes, queryFn: listEventTypes })
}

export function useEventType(id: string) {
  return useQuery({ queryKey: queryKeys.eventType(id), queryFn: () => getEventType(id) })
}

export function useSlots(id: string) {
  return useQuery({ queryKey: queryKeys.slots(id), queryFn: () => listSlots(id) })
}

export function useBookings() {
  return useQuery({ queryKey: queryKeys.bookings, queryFn: listBookings })
}

export function useCreateBooking(eventTypeId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createBooking,
    // Занятость календаря общая для всех типов событий, поэтому после любой
    // попытки — удачной или нет — слоты и список встреч могли устареть.
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.slots(eventTypeId) })
      void queryClient.invalidateQueries({ queryKey: queryKeys.bookings })
    },
  })
}

export function useCreateEventType() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createEventType,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.eventTypes })
    },
  })
}

export function useCancelBooking() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: cancelBooking,
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.bookings })
      // Освободившийся слот влияет на доступность времени всех типов событий,
      // поэтому перезапрашиваем слоты по всем типам.
      void queryClient.invalidateQueries({
        predicate: ({ queryKey }) =>
          queryKey[0] === 'event-types' && queryKey[2] === 'slots',
      })
    },
  })
}
