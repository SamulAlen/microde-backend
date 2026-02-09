// Global initialization and configuration
export const qiankun = {
  // Application lifecycle hooks
  async bootstrap() {
    console.log('Microde app bootstrap');
  },
  async mount(props: any) {
    console.log('Microde app mount', props);
  },
  async unmount(props: any) {
    console.log('Microde app unmount', props);
  },
};
