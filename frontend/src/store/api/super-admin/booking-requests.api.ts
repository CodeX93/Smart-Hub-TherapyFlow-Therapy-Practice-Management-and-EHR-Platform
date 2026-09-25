import { baseApi } from "../baseApi";

export interface BookingRequestDto {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  practiceName: string;
  servicesOffered: string;
  practiceSize: string;
  country?: string;
  address?: string;
  status: "PENDING" | "REVIEWED" | "CONVERTED" | "REJECTED";
  statusMessage?: string;
  createdAt: string;
}

export interface BookingRequestListResponse {
  content: BookingRequestDto[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const superAdminBookingRequestsApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getBookingRequests: builder.query<BookingRequestListResponse, { page: number; size: number }>({
      query: ({ page, size }) => `/api/v1/super-admin/booking-requests?page=${page}&size=${size}`,
      providesTags: ["BookingRequests"],
    }),
    getBookingRequestById: builder.query<BookingRequestDto, number>({
      query: (id) => `/api/v1/super-admin/booking-requests/${id}`,
      providesTags: (_result, _error, id) => [{ type: "BookingRequests", id }],
    }),
    updateBookingRequestStatus: builder.mutation<BookingRequestDto, { id: number; status: string; statusMessage?: string }>({
      query: ({ id, status, statusMessage }) => {
        let url = `/api/v1/super-admin/booking-requests/${id}/status?status=${status}`;
        if (statusMessage) {
          url += `&statusMessage=${encodeURIComponent(statusMessage)}`;
        }
        return {
          url,
          method: "PATCH",
        };
      },
      invalidatesTags: ["BookingRequests", { type: "BookingRequests", id: "LIST" }],
    }),
    deleteBookingRequest: builder.mutation<void, number>({
      query: (id) => ({
        url: `/api/v1/super-admin/booking-requests/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: ["BookingRequests"],
    }),
  }),
});

export const {
  useGetBookingRequestsQuery,
  useGetBookingRequestByIdQuery,
  useUpdateBookingRequestStatusMutation,
  useDeleteBookingRequestMutation,
} = superAdminBookingRequestsApi;
