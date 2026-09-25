import { baseApi } from "../baseApi";

export interface TaskItem {
  id: number;
  title: string;
  titleKey?: string | null;
  taskType?: string | null;
  description?: string;
  status: string;
  priority: string;
  dueDate?: string;
  clientId?: number;
  clientName?: string;
  assignedToId?: number;
  assignedToName?: string;
  commentCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface TasksResponse {
  items: TaskItem[];
  totalCount: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export interface TasksQueryParams {
  page?: number;
  pageSize?: number;
  status?: string;
  priority?: string;
  assignedToId?: number;
  clientId?: number;
  search?: string;
  dateFilter?: string;
  fromDate?: string;
  toDate?: string;
  dateField?: "dueDate" | "createdAt" | "updatedAt";
  sortBy?: string;
  sortOrder?: string;
}

export interface TaskCommentItem {
  id: number;
  content: string;
  isInternal: boolean;
  authorId: number;
  authorName: string;
  createdAt: string;
}

export interface TaskStatsResponse {
  totalTasks: number;
  pendingTasks: number;
  inProgressTasks: number;
  completedTasks: number;
  overdueTasks: number;
  needsAttentionTasks: number;
  highPriorityTasks: number;
  urgentTasks: number;
}

export interface TaskStatsQueryParams {
  assignedToId?: number;
  fromDate?: string;
  toDate?: string;
  dateField?: "dueDate" | "createdAt" | "updatedAt";
}

export const tasksApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getTasks: builder.query<TasksResponse, TasksQueryParams>({
      query: (params) => ({
        url: "/api/v1/tasks",
        method: "GET",
        params,
      }),
      providesTags: (result) =>
        result
          ? [
              ...result.items.map((task) => ({
                type: "Tasks" as const,
                id: task.id,
              })),
              { type: "Tasks", id: "LIST" },
            ]
          : [{ type: "Tasks", id: "LIST" }],
    }),
    getTaskStats: builder.query<TaskStatsResponse, TaskStatsQueryParams | void>({
      query: (params) => ({
        url: "/api/v1/tasks/stats",
        method: "GET",
        params: params || undefined,
      }),
      providesTags: [{ type: "Tasks", id: "STATS" }],
    }),
    getTaskById: builder.query<TaskItem, number>({
      query: (id) => ({
        url: `/api/v1/tasks/${id}`,
        method: "GET",
      }),
      providesTags: (_result, _error, id) => [{ type: "Tasks", id }],
    }),
    getTaskHistory: builder.query<TasksResponse, TasksQueryParams>({
      query: (params) => ({
        url: "/api/v1/tasks/history",
        method: "GET",
        params,
      }),
      providesTags: [{ type: "Tasks", id: "HISTORY" }],
    }),
    createTask: builder.mutation<TaskItem, Partial<TaskItem>>({
      query: (body) => ({
        url: "/api/v1/tasks",
        method: "POST",
        body,
      }),
      invalidatesTags: [
        { type: "Tasks", id: "LIST" },
        { type: "Tasks", id: "HISTORY" },
        { type: "Tasks", id: "STATS" },
        { type: "Tasks", id: "PENDING_COUNT" },
      ],
    }),
    replaceTask: builder.mutation<TaskItem, { id: number; body: Partial<TaskItem> }>({
      query: ({ id, body }) => ({
        url: `/api/v1/tasks/${id}`,
        method: "PUT",
        body,
      }),
      invalidatesTags: (_result, _error, { id }) => [
        { type: "Tasks", id },
        { type: "Tasks", id: "LIST" },
        { type: "Tasks", id: "HISTORY" },
        { type: "Tasks", id: "STATS" },
        { type: "Tasks", id: "PENDING_COUNT" },
      ],
    }),
    updateTask: builder.mutation<TaskItem, { id: number; body: Partial<TaskItem> }>({
      query: ({ id, body }) => ({
        url: `/api/v1/tasks/${id}`,
        method: "PATCH",
        body,
      }),
      invalidatesTags: (_result, _error, { id }) => [
        { type: "Tasks", id },
        { type: "Tasks", id: "LIST" },
        { type: "Tasks", id: "HISTORY" },
        { type: "Tasks", id: "STATS" },
        { type: "Tasks", id: "PENDING_COUNT" },
      ],
    }),
    deleteTask: builder.mutation<{ success: boolean }, number>({
      query: (id) => ({
        url: `/api/v1/tasks/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: (_result, _error, id) => [
        { type: "Tasks", id },
        { type: "Tasks", id: "LIST" },
        { type: "Tasks", id: "HISTORY" },
        { type: "Tasks", id: "STATS" },
        { type: "Tasks", id: "PENDING_COUNT" },
      ],
    }),
    getTaskComments: builder.query<TaskCommentItem[], number>({
      query: (taskId) => ({
        url: `/api/v1/tasks/${taskId}/comments`,
        method: "GET",
      }),
      providesTags: (_result, _error, taskId) => [
        { type: "Tasks", id: `COMMENTS_${taskId}` },
      ],
    }),
    createTaskComment: builder.mutation<
      TaskCommentItem,
      { taskId: number; content: string; isInternal: boolean }
    >({
      query: ({ taskId, content, isInternal }) => ({
        url: `/api/v1/tasks/${taskId}/comments`,
        method: "POST",
        body: { content, isInternal },
      }),
      invalidatesTags: (_result, _error, { taskId }) => [
        { type: "Tasks", id: `COMMENTS_${taskId}` },
        { type: "Tasks", id: taskId },
        { type: "Tasks", id: "LIST" },
      ],
    }),
    updateTaskComment: builder.mutation<
      TaskCommentItem,
      { taskId: number; commentId: number; content: string; isInternal: boolean }
    >({
      query: ({ taskId, commentId, content, isInternal }) => ({
        url: `/api/v1/tasks/${taskId}/comments/${commentId}`,
        method: "PUT",
        body: { content, isInternal },
      }),
      invalidatesTags: (_result, _error, { taskId }) => [
        { type: "Tasks", id: `COMMENTS_${taskId}` },
        { type: "Tasks", id: taskId },
      ],
    }),
    deleteTaskComment: builder.mutation<void, { taskId: number; commentId: number }>({
      query: ({ taskId, commentId }) => ({
        url: `/api/v1/tasks/${taskId}/comments/${commentId}`,
        method: "DELETE",
      }),
      invalidatesTags: (_result, _error, { taskId }) => [
        { type: "Tasks", id: `COMMENTS_${taskId}` },
        { type: "Tasks", id: taskId },
        { type: "Tasks", id: "LIST" },
      ],
    }),
  }),
});

export const { 
  useGetTasksQuery,
  useGetTaskStatsQuery,
  useLazyGetTaskByIdQuery,
  useGetTaskHistoryQuery,
  useCreateTaskMutation,
  useReplaceTaskMutation,
  useUpdateTaskMutation,
  useDeleteTaskMutation,
  useLazyGetTaskCommentsQuery,
  useCreateTaskCommentMutation,
  useUpdateTaskCommentMutation,
  useDeleteTaskCommentMutation,
} = tasksApi;
