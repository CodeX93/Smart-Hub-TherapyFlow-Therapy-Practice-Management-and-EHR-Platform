export const AUTH_STATIC_CONTENT = {
    loginTitle: 'Welcome back to SmartHub 👋',
    rightBoxTitle: 'Access your appointments, billing, and documents - all in one secure place.',
    newClientGuide: 'New client? Contact your therapist to activate your portal access.',
    services: [
        {
            id: 1,
            title: 'Book Appointments',
            description: 'View available time slots and schedule sessions with your therapist',
            status: 'active'
        },
        {
            id: 2,
            title: 'View Invoices',
            description: 'Access billing history and payment status anytime',
            status: 'active'
        },
        {
            id: 3,
            title: 'Upload Documents',
            description: 'Securely share insurance cards, forms, and other documents',
            status: 'active'
        },
        {
            id: 4,
            title: 'HIPAA Secure',
            description: 'Your information is protected with industry-leading security',
            status: 'active'
        }
    ]
}

export const staticUsers = [
    {
        id: 1,
        email: 'user@gmail.com',
        password: 'user1234',
        role: 'user'
    },
    {
        id: 2,
        email: 'therapist@gmail.com',
        password: 'therapist1234',
        role: 'therapist'
    },
    {
        id: 3,
        email: 'admin@gmail.com',
        password: 'admin1234',
        role: 'admin'
    },
    {
        id: 4,
        email: 'super-admin@gmail.com',
        password: 'superadmin1234',
        role: 'super-admin'
    }
]