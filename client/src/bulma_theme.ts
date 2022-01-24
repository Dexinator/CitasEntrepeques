import { Theme, createPlugin } from '@fullcalendar/common';

export class BulmaTheme extends Theme {}

BulmaTheme.prototype.classes = {
  button: 'button is-primary',
  buttonActive: 'is-active',
  buttonGroup: 'buttons',
  popover: 'card',
  popoverContent: 'card-body',
  popoverHeader: 'card-header',
  root: 'fc-theme-bulma',
  table: 'table is-bordered', // don't attache the `table` class. we only want the borders, not any layout
  tableCellShaded: 'has-background-grey-lighter',
};

const plugin = createPlugin({
  themeClasses: {
    bulma: BulmaTheme,
  },
});

export default plugin;
